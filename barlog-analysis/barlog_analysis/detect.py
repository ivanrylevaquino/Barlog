"""Heuristic exercise detection from pose keypoints (no training).

Looks at where the body is (horizontal, hanging, upright, supported) and which joint moves the most.
It can be wrong, so the app always lets the user correct the exercise.
"""
from __future__ import annotations

from statistics import median
from typing import Optional

from .geometry import SIDES, angle, tilt_from_horizontal
from .rules import MIN_CONF, Frame

ALL = ("shoulder", "elbow", "wrist", "hip", "knee", "ankle")


def _side(frames: list[Frame]) -> str:
    score = {s: sum(f.kpts[i][2] for f in frames if f.kpts for i in idx.values()) for s, idx in SIDES.items()}
    return max(score, key=score.get)


def _visible(f: Frame, side: str, names) -> Optional[dict]:
    if f.kpts is None:
        return None
    j = {n: f.kpts[i] for n, i in SIDES[side].items()}
    return j if all(j[n][2] >= MIN_CONF for n in names) else None


def _spread(values: list[float]) -> float:
    if len(values) < 4:
        return 0.0
    v = sorted(values)
    lo, hi = v[int(len(v) * 0.1)], v[int(len(v) * 0.9)]
    return hi - lo


def detect(frames: list[Frame]) -> tuple[Optional[str], float, dict]:
    """Returns (exercise, confidence 0..1, debug features). exercise is None when nothing fits."""
    tracked = [f for f in frames if f.kpts is not None]
    if len(tracked) < 5:
        return None, 0.0, {}
    side = _side(tracked)
    js = [j for j in (_visible(f, side, ("shoulder", "elbow", "wrist", "hip")) for f in tracked) if j]
    if len(js) < 5:
        return None, 0.0, {}

    elbow = [angle(j["shoulder"], j["elbow"], j["wrist"]) for j in js]
    legs = [j for j in js if j["knee"][2] >= MIN_CONF and j["ankle"][2] >= MIN_CONF]
    knee = [angle(j["hip"], j["knee"], j["ankle"]) for j in legs]
    hipang = [angle(j["shoulder"], j["hip"], j["knee"]) for j in legs]
    body_tilt = [tilt_from_horizontal(j["shoulder"], j["ankle"]) for j in legs]
    leg_tilt = [tilt_from_horizontal(j["hip"], j["ankle"]) for j in legs]
    hanging = sum(1 for j in js if j["wrist"][1] < j["shoulder"][1]) / len(js)

    f = {
        "elbowRange": round(_spread(elbow), 1), "kneeRange": round(_spread(knee), 1),
        "hanging": round(hanging, 2),
        "bodyTilt": round(median(body_tilt), 1) if body_tilt else None,
        "legTilt": round(median(leg_tilt), 1) if leg_tilt else None,
        "hipAngle": round(median(hipang), 1) if hipang else None,
    }
    moving_arms = f["elbowRange"] >= 40
    moving_legs = f["kneeRange"] >= 40

    if hanging > 0.6:
        return ("PULL_UP", 0.85, f) if moving_arms else ("DEAD_HANG", 0.75, f)
    if f["bodyTilt"] is not None and f["bodyTilt"] < 35:
        return ("PUSH_UP", 0.8, f) if moving_arms else ("PLANK", 0.7, f)
    if moving_legs and f["kneeRange"] >= f["elbowRange"]:
        return "SQUAT", 0.75, f
    if f["hipAngle"] is not None and 60 <= f["hipAngle"] <= 120 and f["legTilt"] is not None and f["legTilt"] < 35:
        return "L_SIT", 0.6, f
    if moving_arms:
        return "DIP", 0.6, f
    return None, 0.2, f
