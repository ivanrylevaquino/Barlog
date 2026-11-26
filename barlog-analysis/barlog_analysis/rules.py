"""Rule-based rep counting, hold timing and form feedback from pose keypoints.

No training involved: a pretrained pose model supplies body keypoints per frame, and this module turns
joint angles into reps, hold time and plain-English feedback (SRS FR-017..FR-020, NFR-008).

The rep counter uses the same idea as Ultralytics' AIGym workout monitor
(https://docs.ultralytics.com/guides/workouts-monitoring, AGPL-3.0): track the angle at a joint and count
a rep each time it moves from above an "up" threshold to below a "down" threshold. It is re-implemented
here so it works offline on whole clips, per exercise, and returns structured results.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field
from typing import Callable, Optional, Sequence

from .geometry import (NOSE, SIDES, angle, dist, fmt_time, signed_offset_from_line, smooth,
                       tilt_from_horizontal, tilt_from_vertical)

MIN_CONF = 0.5          # keypoint confidence needed to trust a joint
MIN_TRACKED = 0.4       # share of frames that must contain a person
MIN_JOINTS = 0.5        # share of tracked frames where the exercise's key joints are visible
DARK = 60               # mean frame brightness (0-255) below which we blame lighting


@dataclass
class Frame:
    t: float                                   # seconds from start of clip
    kpts: Optional[Sequence[Sequence[float]]]  # 17 x (x, y, conf), or None if nobody was found


@dataclass
class Rep:
    start: float
    bottom_t: float
    bottom_angle: float
    lockout_before: Optional[float] = None
    lockout_after: Optional[float] = None
    bottom: dict = field(default_factory=dict)   # exercise-specific measures at the deepest point
    worst: dict = field(default_factory=dict)    # worst value seen during the rep


# ── Exercise definitions ────────────────────────────────────────────────

@dataclass
class RepExercise:
    joints: tuple[str, ...]                    # joints that must be visible
    primary: tuple[str, str, str]              # angle that drives the rep counter
    extended: float                            # "up" threshold (arms/legs straight)
    flexed: float                              # "down" threshold (bottom of the rep)
    measures: Callable[[dict], dict]           # per-frame extra measures for form checks
    checks: list                               # list of Check


@dataclass
class HoldExercise:
    joints: tuple[str, ...]
    in_position: Callable[[dict], bool]
    measures: Callable[[dict], dict]
    checks: list
    label: str


@dataclass
class Check:
    """A form rule evaluated once per rep (or once per hold)."""
    ok: Callable[[Rep], Optional[bool]]   # None = could not judge
    good: str
    bad: str
    severe: bool = False                  # failing on most reps is an ERROR rather than a WARNING


def _body_line(j):
    return angle(j["shoulder"], j["hip"], j["ankle"])


def _sag(j):
    """Positive when hips drop below the shoulder-ankle line, negative when they pike up; in torso lengths."""
    torso = dist(j["shoulder"], j["hip"]) or 1.0
    return signed_offset_from_line(j["hip"], j["shoulder"], j["ankle"]) / torso


EXERCISES: dict[str, object] = {
    "PUSH_UP": RepExercise(
        joints=("shoulder", "elbow", "wrist", "hip", "ankle"),
        primary=("shoulder", "elbow", "wrist"), extended=145, flexed=110,
        measures=lambda j: {"line": _body_line(j), "sag": _sag(j)},
        checks=[
            Check(lambda r: r.bottom_angle <= 100,
                  "Elbows reach about 90° at the bottom of your reps.",
                  "Depth is shallow. Lower until your elbows bend to about 90°.", severe=True),
            Check(lambda r: None if r.worst.get("sag") is None else r.worst["sag"] < 0.18,
                  "Your body stays in one straight line from shoulders to ankles.",
                  "Your hips sag toward the floor. Squeeze your glutes and brace your core."),
            Check(lambda r: None if r.worst.get("pike") is None else r.worst["pike"] > -0.22,
                  "Hips stay level instead of piking up.",
                  "Your hips pike up. Keep them in line with your shoulders and ankles."),
            Check(lambda r: None if r.lockout_after is None else r.lockout_after >= 155,
                  "You lock out fully at the top of each rep.",
                  "Push all the way up until your arms are straight at the top."),
        ],
    ),
    "PULL_UP": RepExercise(
        joints=("shoulder", "elbow", "wrist", "hip"),
        primary=("shoulder", "elbow", "wrist"), extended=145, flexed=80,
        measures=lambda j: {"chin_over": j["nose"] is not None and j["nose"][1] < min(j["wrist"][1], j["wrist2"][1] if j["wrist2"] else j["wrist"][1]),
                            "hip_x": j["hip"][0], "torso": dist(j["shoulder"], j["hip"])},
        checks=[
            Check(lambda r: r.bottom.get("chin_over"),
                  "Your chin clears the bar on your reps.",
                  "Your chin doesn't clear the bar. Pull until it's above your hands.", severe=True),
            Check(lambda r: None if r.lockout_before is None else r.lockout_before >= 150,
                  "You start each rep from a full dead hang.",
                  "Start each rep from straight arms instead of a half hang."),
            Check(lambda r: None if r.worst.get("swing") is None else r.worst["swing"] < 0.45,
                  "Your body stays still with no kipping.",
                  "Your body swings on the way up. Keep your core tight and legs still."),
        ],
    ),
    "SQUAT": RepExercise(
        joints=("shoulder", "hip", "knee", "ankle"),
        primary=("hip", "knee", "ankle"), extended=155, flexed=110,
        measures=lambda j: {"hip_below_knee": j["hip"][1] >= j["knee"][1] - 0.05 * dist(j["hip"], j["knee"]),
                            "lean": tilt_from_vertical(j["shoulder"], j["hip"])},
        checks=[
            Check(lambda r: r.bottom.get("hip_below_knee"),
                  "Your hips drop to knee height or lower.",
                  "Squat deeper. Lower until your hips are level with your knees.", severe=True),
            Check(lambda r: None if r.bottom.get("lean") is None else r.bottom["lean"] <= 50,
                  "Your chest stays up through the squat.",
                  "Your chest drops forward at the bottom. Keep it proud and sit back."),
            Check(lambda r: None if r.lockout_after is None else r.lockout_after >= 160,
                  "You stand all the way up between reps.",
                  "Stand all the way up at the top of each rep."),
        ],
    ),
    "DIP": RepExercise(
        joints=("shoulder", "elbow", "wrist", "hip"),
        primary=("shoulder", "elbow", "wrist"), extended=145, flexed=110,
        measures=lambda j: {"lean": tilt_from_vertical(j["shoulder"], j["hip"])},
        checks=[
            Check(lambda r: r.bottom_angle <= 100,
                  "Elbows reach about 90° with steady control.",
                  "You stop short of 90°. Lower a little further on each rep.", severe=True),
            Check(lambda r: None if r.bottom.get("lean") is None else r.bottom["lean"] <= 45,
                  "Your torso stays fairly upright.",
                  "Your torso leans forward a lot. Stay more upright to protect your shoulders."),
            Check(lambda r: None if r.lockout_after is None else r.lockout_after >= 155,
                  "Full lockout at the top.",
                  "Press all the way up until your arms are straight."),
        ],
    ),
    "PLANK": HoldExercise(
        joints=("shoulder", "hip", "ankle"), label="plank",
        in_position=lambda j: tilt_from_horizontal(j["shoulder"], j["ankle"]) < 35 and _body_line(j) > 150,
        measures=lambda j: {"sag": _sag(j)},
        checks=[
            Check(lambda r: None if r.worst.get("sag") is None else r.worst["sag"] < 0.18,
                  "Your body stays in one straight line.",
                  "Your hips sag during the hold. Squeeze your glutes and tuck your pelvis."),
            Check(lambda r: None if r.worst.get("pike") is None else r.worst["pike"] > -0.22,
                  "Hips stay level instead of piking up.",
                  "Your hips creep up. Lower them in line with your shoulders and ankles."),
        ],
    ),
    "DEAD_HANG": HoldExercise(
        joints=("shoulder", "elbow", "wrist"), label="dead hang",
        in_position=lambda j: j["wrist"][1] < j["shoulder"][1] and angle(j["shoulder"], j["elbow"], j["wrist"]) > 145,
        measures=lambda j: {"elbow": angle(j["shoulder"], j["elbow"], j["wrist"])},
        checks=[
            Check(lambda r: None if r.worst.get("elbow_min") is None else r.worst["elbow_min"] >= 155,
                  "Arms stay fully straight through the hang.",
                  "Your elbows bend during the hang. Relax into straight arms."),
        ],
    ),
    "L_SIT": HoldExercise(
        joints=("shoulder", "hip", "knee", "ankle", "wrist"), label="L-sit",
        in_position=lambda j: 60 <= angle(j["shoulder"], j["hip"], j["knee"]) <= 120
                              and tilt_from_horizontal(j["hip"], j["ankle"]) < 35 and j["wrist"][1] > j["shoulder"][1],
        measures=lambda j: {"knee": angle(j["hip"], j["knee"], j["ankle"])},
        checks=[
            Check(lambda r: None if r.worst.get("knee_min") is None else r.worst["knee_min"] >= 150,
                  "Legs stay straight through the hold.",
                  "Your knees bend. Lock your legs straight, or tuck on purpose as a regression."),
        ],
    ),
}

SUPPORTED = sorted(EXERCISES)


# ── Joint extraction ────────────────────────────────────────────────────

def _pick_side(frames: list[Frame], joints: tuple[str, ...]) -> str:
    score = {"left": 0.0, "right": 0.0}
    for f in frames:
        if f.kpts is None:
            continue
        for side, idx in SIDES.items():
            score[side] += sum(f.kpts[idx[j]][2] for j in joints)
    return max(score, key=score.get)


def _joints(f: Frame, side: str, needed: tuple[str, ...]) -> Optional[dict]:
    if f.kpts is None:
        return None
    idx = SIDES[side]
    other = SIDES["right" if side == "left" else "left"]
    j = {name: f.kpts[i] for name, i in idx.items()}
    if any(j[n][2] < MIN_CONF for n in needed):
        return None
    j["nose"] = f.kpts[NOSE] if f.kpts[NOSE][2] >= MIN_CONF else None
    w2 = f.kpts[other["wrist"]]
    j["wrist2"] = w2 if w2[2] >= MIN_CONF else None
    return j


# ── Public entry point ──────────────────────────────────────────────────

def analyze(frames: list[Frame], exercise: str, brightness: Optional[float] = None) -> dict:
    ex = EXERCISES.get(exercise)
    if ex is None:
        raise ValueError(f"Unsupported exercise '{exercise}'. Supported: {', '.join(SUPPORTED)}")

    tracked = [f for f in frames if f.kpts is not None]
    if not frames or len(tracked) / len(frames) < MIN_TRACKED:
        if brightness is not None and brightness < DARK:
            return _failed("POOR_LIGHTING", "The video was too dark for us to follow your movement.")
        return _failed("DETECTION_FAILED", "We could not find a person in most of this video.")

    side = _pick_side(tracked, ex.joints)
    joints = [(f.t, _joints(f, side, ex.joints)) for f in frames]
    visible = sum(1 for _, j in joints if j is not None)
    if visible / len(tracked) < MIN_JOINTS:
        return _failed("OCCLUSION", "Part of your body was out of frame or hidden for most of the set.")

    if isinstance(ex, RepExercise):
        result = _analyze_reps(ex, joints)
    else:
        result = _analyze_hold(ex, joints)
    result["metrics"].update(framesAnalyzed=len(frames), trackedRatio=round(len(tracked) / len(frames), 2), side=side)
    return result


def _failed(reason: str, summary: str) -> dict:
    return {"status": "FAILED", "failureReason": reason, "overall": "FAILED", "repsDetected": 0, "partialReps": 0,
            "holdSeconds": None, "summary": summary, "items": [], "repEvents": [], "metrics": {}}


# ── Reps ────────────────────────────────────────────────────────────────

def _analyze_reps(ex: RepExercise, joints: list[tuple[float, Optional[dict]]]) -> dict:
    a, b, c = ex.primary
    raw = [angle(j[a], j[b], j[c]) if j else None for _, j in joints]
    angles = smooth(raw)
    measures = [ex.measures(j) if j else None for _, j in joints]

    reps: list[Rep] = []
    partial: list[float] = []
    stage = None              # None until we first see the extended position
    top_max = None            # best extension in the current "up" phase
    dip_min = None            # lowest angle in an up-phase that never reached "flexed"
    for i, ((t, _), ang) in enumerate(zip(joints, angles)):
        if ang is None:
            continue
        if stage is None:
            if ang >= ex.extended:
                stage, top_max = "up", ang
            continue
        if stage == "up":
            if ang >= ex.extended:
                if dip_min is not None and dip_min < ex.extended - 15:
                    partial.append(t)          # went partway down and came back: not a rep
                dip_min = None
                top_max = max(top_max, ang)
            elif ang < ex.flexed:              # reached the bottom: count the rep (AIGym-style)
                if reps:
                    reps[-1].lockout_after = top_max
                reps.append(Rep(start=t, bottom_t=t, bottom_angle=ang, lockout_before=top_max))
                stage, top_max, dip_min = "down", None, None
            else:
                dip_min = ang if dip_min is None else min(dip_min, ang)
        else:  # stage == "down"
            rep = reps[-1]
            if ang < rep.bottom_angle:
                rep.bottom_angle, rep.bottom_t = ang, t
            if ang >= ex.extended:
                stage, top_max = "up", ang
        if stage == "down" and measures[i] is not None:
            _track(reps[-1], measures[i], ang)
    if reps and reps[-1].lockout_after is None and stage == "up":
        reps[-1].lockout_after = top_max

    # Swing (pull-ups): horizontal hip travel relative to torso length, per rep.
    for rep in reps:
        xs = [m["hip_x"] for (t, _), m in zip(joints, measures)
              if m and "hip_x" in m and rep.start - 0.5 <= t <= rep.bottom_t + 1.0]
        torso = [m["torso"] for m in measures if m and "torso" in m]
        if xs and torso:
            rep.worst["swing"] = (max(xs) - min(xs)) / (sorted(torso)[len(torso) // 2] or 1)

    valid = [x for x in angles if x is not None]
    items = []
    if not reps:
        span = (max(valid) - min(valid)) if valid else 0
        text = ("We couldn't see a full rep. Film from the side with your whole body in frame."
                if span < 25 else
                "We saw movement but no full reps. Go through the full range so each rep counts.")
        return _result(0, None, [{"type": "ERROR", "t": "0:00", "text": text}], ex_label=None,
                       metrics={"minAngle": _r(min(valid) if valid else None), "maxAngle": _r(max(valid) if valid else None)})

    for check in ex.checks:
        verdicts = [(r, check.ok(r)) for r in reps]
        judged = [(r, v) for r, v in verdicts if v is not None]
        if not judged:
            continue
        failed = [r for r, v in judged if not v]
        if not failed:
            items.append({"type": "POSITIVE", "t": fmt_time(reps[0].bottom_t), "text": check.good})
        else:
            share = len(failed) / len(judged)
            kind = "ERROR" if check.severe and share > 0.5 else "WARNING"
            where = "on every rep" if len(failed) == len(judged) else f"on {len(failed)} of {len(judged)} reps"
            items.append({"type": kind, "t": fmt_time(failed[0].bottom_t), "text": f"{check.bad} ({where})"})
    if partial:
        items.append({"type": "WARNING", "t": fmt_time(partial[0]),
                      "text": f"{len(partial)} rep{'s' if len(partial) > 1 else ''} didn't go deep enough to count."})

    items.sort(key=lambda it: {"ERROR": 0, "WARNING": 1, "POSITIVE": 2}[it["type"]])
    out = _result(len(reps), None, items, ex_label=None,
                  metrics={"minAngle": _r(min(valid)), "maxAngle": _r(max(valid)), "partialReps": len(partial)})
    out["partialReps"] = len(partial)
    out["repEvents"] = sorted([{"t": round(r.bottom_t, 2), "counted": True} for r in reps]
                              + [{"t": round(t, 2), "counted": False} for t in partial], key=lambda e: e["t"])
    return out


def _track(rep: Rep, m: dict, ang: float) -> None:
    if ang <= rep.bottom_angle + 1e-9:
        rep.bottom = dict(m)
    for k, v in m.items():
        if isinstance(v, bool) or v is None or (isinstance(v, float) and math.isnan(v)):
            continue
        if k == "sag":
            rep.worst["sag"] = max(rep.worst.get("sag", -9), v)
            rep.worst["pike"] = min(rep.worst.get("pike", 9), v)


# ── Holds ───────────────────────────────────────────────────────────────

GAP = 0.6   # seconds of lost position tolerated inside one hold


def _analyze_hold(ex: HoldExercise, joints: list[tuple[float, Optional[dict]]]) -> dict:
    best = (0.0, None, None)  # duration, start, end
    start = last_ok = None
    for t, j in joints:
        ok = j is not None and ex.in_position(j)
        if ok:
            if start is None or (last_ok is not None and t - last_ok > GAP):
                start = t
            last_ok = t
            if last_ok - start > best[0]:
                best = (last_ok - start, start, last_ok)
    duration, s, e = best
    if duration < 1.0:
        return _result(None, 0.0, [{"type": "ERROR", "t": "0:00",
                       "text": f"We couldn't find a steady {ex.label}. Film from the side and hold still for a few seconds."}],
                       ex_label=ex.label, metrics={})

    hold = Rep(start=s, bottom_t=s, bottom_angle=0)
    for t, j in joints:
        if j is None or not (s <= t <= e):
            continue
        m = ex.measures(j)
        if "sag" in m:
            hold.worst["sag"] = max(hold.worst.get("sag", -9), m["sag"])
            hold.worst["pike"] = min(hold.worst.get("pike", 9), m["sag"])
        if "elbow" in m:
            hold.worst["elbow_min"] = min(hold.worst.get("elbow_min", 999), m["elbow"])
        if "knee" in m:
            hold.worst["knee_min"] = min(hold.worst.get("knee_min", 999), m["knee"])

    items = [{"type": "POSITIVE", "t": fmt_time(s), "text": f"You held the {ex.label} for {duration:.1f} seconds."}]
    for check in ex.checks:
        v = check.ok(hold)
        if v is None:
            continue
        items.append({"type": "POSITIVE" if v else "WARNING", "t": fmt_time(s), "text": check.good if v else check.bad})
    items.sort(key=lambda it: {"ERROR": 0, "WARNING": 1, "POSITIVE": 2}[it["type"]])
    out = _result(None, round(duration, 1), items, ex_label=ex.label, metrics={"holdStart": round(s, 2), "holdEnd": round(e, 2)})
    out["holdWindow"] = {"start": round(s, 2), "end": round(e, 2)}
    return out


# ── Result envelope ─────────────────────────────────────────────────────

def _result(reps: Optional[int], hold: Optional[float], items: list[dict], ex_label, metrics: dict) -> dict:
    errors = sum(1 for i in items if i["type"] == "ERROR")
    warnings = sum(1 for i in items if i["type"] == "WARNING")
    good = errors == 0 and warnings <= 1
    if hold is not None:
        count = f"{hold:.1f}-second {ex_label}" if hold else "No steady hold found"
    else:
        count = f"{reps} rep{'s' if reps != 1 else ''} counted" if reps else "No full reps counted"
    if errors:
        tail = "Work on the first tip next session."
    elif warnings:
        tail = "Good effort, with " + ("one thing" if warnings == 1 else f"{warnings} things") + " to watch."
    else:
        tail = "Your form held up well."
    return {"status": "COMPLETED", "failureReason": None, "overall": "GOOD" if good else "NEEDS_IMPROVEMENT",
            "repsDetected": reps or 0, "partialReps": 0, "holdSeconds": hold, "summary": f"{count}. {tail}",
            "items": items, "repEvents": [], "metrics": metrics}


def _r(v):
    return None if v is None else round(v, 1)
