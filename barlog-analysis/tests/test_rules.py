"""Rule tests on synthetic side-view skeletons (no model, no video needed)."""
import math

import pytest

from barlog_analysis.geometry import angle
from barlog_analysis.rules import Frame, analyze

FPS = 12
R = dict(nose=0, shoulder=6, elbow=8, wrist=10, hip=12, knee=14, ankle=16)


def kp(points, conf=0.9, low=()):
    """Build a 17-keypoint list with the right-side joints set; everything else low confidence."""
    k = [(0.0, 0.0, 0.05)] * 17
    for name, (x, y) in points.items():
        k[R[name]] = (x, y, 0.1 if name in low else conf)
    return k


def rep_angles(n, top, bottom, rep_s=1.5, rest_s=0.4, lead_s=0.5):
    """Angle trace for n reps: top -> bottom -> top, with short pauses."""
    out = [top] * int(lead_s * FPS)
    for _ in range(n):
        steps = int(rep_s * FPS)
        for i in range(steps + 1):
            out.append(bottom + (top - bottom) * (1 + math.cos(2 * math.pi * i / steps)) / 2)
        out += [top] * int(rest_s * FPS)
    return out


def frames_from(poses):
    return [Frame(t=i / FPS, kpts=p) for i, p in enumerate(poses)]


def _bend(a, b, theta, length, side):
    """Point that makes angle a-b-new = theta at b, `length` away from b, on the chosen side."""
    ux, uy = a[0] - b[0], a[1] - b[1]
    n = math.hypot(ux, uy)
    ux, uy = ux / n, uy / n
    t = math.radians(theta) * side
    return (b[0] + length * (ux * math.cos(t) - uy * math.sin(t)),
            b[1] + length * (ux * math.sin(t) + uy * math.cos(t)))


def _arm(wrist, theta, up, L=40):
    """Shoulder straight above (up=True) or below the wrist, elbow pushed sideways, elbow angle = theta."""
    d = 2 * L * math.sin(math.radians(theta) / 2)
    shoulder = (wrist[0], wrist[1] - d if up else wrist[1] + d)
    mid = ((wrist[0] + shoulder[0]) / 2, (wrist[1] + shoulder[1]) / 2)
    off = L * math.cos(math.radians(theta) / 2)
    elbow = (mid[0] - off, mid[1])
    return shoulder, elbow


def pushup(theta, sag=0.0):
    wrist = (500, 400)
    shoulder, elbow = _arm(wrist, theta, up=True)
    ankle = (200, 400)
    hip = ((shoulder[0] + ankle[0]) / 2, (shoulder[1] + ankle[1]) / 2 + sag)
    knee = ((hip[0] + ankle[0]) / 2, (hip[1] + ankle[1]) / 2)
    return kp(dict(nose=(shoulder[0] + 20, shoulder[1]), shoulder=shoulder, elbow=elbow, wrist=wrist,
                   hip=hip, knee=knee, ankle=ankle))


def pullup(theta, swing=0.0):
    wrist = (500, 100)
    shoulder, elbow = _arm(wrist, theta, up=False)
    hip = (shoulder[0] + swing, shoulder[1] + 70)
    return kp(dict(nose=(shoulder[0], shoulder[1] - 35), shoulder=shoulder, elbow=elbow, wrist=wrist,
                   hip=hip, knee=(hip[0], hip[1] + 50), ankle=(hip[0], hip[1] + 100)))


def squat(theta, lean=10):
    ankle = (300, 400)
    knee = (325, 355)
    hip = _bend(ankle, knee, theta, 52, side=1)
    if hip[0] > knee[0]:
        hip = _bend(ankle, knee, theta, 52, side=-1)
    shoulder = (hip[0] + 60 * math.sin(math.radians(lean)), hip[1] - 60 * math.cos(math.radians(lean)))
    return kp(dict(nose=(shoulder[0], shoulder[1] - 20), shoulder=shoulder, elbow=(shoulder[0] + 20, shoulder[1] + 10),
                   wrist=(shoulder[0] + 40, shoulder[1]), hip=hip, knee=knee, ankle=ankle))


def dip(theta, lean=15):
    wrist = (500, 300)
    shoulder, elbow = _arm(wrist, theta, up=True)
    hip = (shoulder[0] - 60 * math.sin(math.radians(lean)), shoulder[1] + 60 * math.cos(math.radians(lean)))
    return kp(dict(nose=(shoulder[0], shoulder[1] - 25), shoulder=shoulder, elbow=elbow, wrist=wrist,
                   hip=hip, knee=(hip[0], hip[1] + 50), ankle=(hip[0], hip[1] + 100)))


def types(result):
    return [i["type"] for i in result["items"]]


# ── sanity check of the synthetic skeleton itself ───────────────────────

def test_synthetic_elbow_angle_is_exact():
    k = pushup(90)
    assert angle(k[6], k[8], k[10]) == pytest.approx(90, abs=0.5)


# ── push-ups ───────────────────────────────────────────────────────────

def test_pushups_good_form():
    r = analyze(frames_from([pushup(a) for a in rep_angles(5, 170, 80)]), "PUSH_UP", brightness=120)
    assert r["status"] == "COMPLETED"
    assert r["repsDetected"] == 5
    assert r["overall"] == "GOOD"
    assert "ERROR" not in types(r) and "WARNING" not in types(r)
    assert r["summary"].startswith("5 reps counted")


def test_pushups_shallow_depth_is_flagged():
    r = analyze(frames_from([pushup(a) for a in rep_angles(4, 170, 104)]), "PUSH_UP")
    assert r["repsDetected"] == 4
    assert r["items"][0]["type"] == "ERROR" and "Depth is shallow" in r["items"][0]["text"]
    assert r["overall"] == "NEEDS_IMPROVEMENT"


def test_pushups_hip_sag_is_flagged():
    r = analyze(frames_from([pushup(a, sag=45) for a in rep_angles(3, 170, 80)]), "PUSH_UP")
    assert r["repsDetected"] == 3
    assert any(i["type"] == "WARNING" and "hips sag" in i["text"] for i in r["items"])


def test_partial_reps_are_not_counted():
    trace = rep_angles(3, 170, 80) + rep_angles(2, 170, 125, lead_s=0)
    r = analyze(frames_from([pushup(a) for a in trace]), "PUSH_UP")
    assert r["repsDetected"] == 3
    assert any("didn't go deep enough" in i["text"] for i in r["items"])


def test_no_movement_gives_zero_reps():
    r = analyze(frames_from([pushup(170)] * 40), "PUSH_UP")
    assert r["repsDetected"] == 0
    assert r["items"][0]["type"] == "ERROR"


# ── pull-ups ───────────────────────────────────────────────────────────

def test_pullups_good():
    r = analyze(frames_from([pullup(a) for a in rep_angles(4, 170, 40)]), "PULL_UP")
    assert r["repsDetected"] == 4
    assert any("chin clears the bar" in i["text"] for i in r["items"] if i["type"] == "POSITIVE")
    assert r["overall"] == "GOOD"


def test_pullups_chin_below_bar_and_half_hang():
    r = analyze(frames_from([pullup(a) for a in [170] * 6 + rep_angles(4, 170, 75, lead_s=0)]), "PULL_UP")
    texts = " ".join(i["text"] for i in r["items"])
    assert "chin doesn't clear the bar" in texts


def test_pullups_swing_is_flagged():
    trace = rep_angles(3, 170, 40)
    poses = [pullup(a, swing=40 * math.sin(i / 3)) for i, a in enumerate(trace)]
    r = analyze(frames_from(poses), "PULL_UP")
    assert any("swings" in i["text"] for i in r["items"])


# ── squats & dips ──────────────────────────────────────────────────────

def test_squats_deep_and_shallow():
    deep = analyze(frames_from([squat(a) for a in rep_angles(3, 175, 50)]), "SQUAT")
    assert deep["repsDetected"] == 3
    assert any("hips drop to knee height" in i["text"] for i in deep["items"] if i["type"] == "POSITIVE")
    shallow = analyze(frames_from([squat(a) for a in rep_angles(3, 175, 105)]), "SQUAT")
    assert shallow["repsDetected"] == 3
    assert any("Squat deeper" in i["text"] for i in shallow["items"])


def test_dips_forward_lean():
    r = analyze(frames_from([dip(a, lean=60) for a in rep_angles(3, 170, 85)]), "DIP")
    assert r["repsDetected"] == 3
    assert any("leans forward" in i["text"] for i in r["items"])


# ── holds ──────────────────────────────────────────────────────────────

def plank(sag=0.0):
    shoulder, ankle = (500, 300), (200, 330)
    hip = (350, 315 + sag)
    return kp(dict(nose=(520, 295), shoulder=shoulder, elbow=(500, 340), wrist=(530, 340), hip=hip,
                   knee=(275, 322 + sag / 2), ankle=ankle))


def standing():
    return kp(dict(nose=(300, 80), shoulder=(300, 110), elbow=(300, 160), wrist=(300, 210),
                   hip=(300, 220), knee=(300, 300), ankle=(300, 380)))


def test_plank_hold_timer():
    poses = [standing()] * 12 + [plank()] * (5 * FPS) + [standing()] * 12
    r = analyze(frames_from(poses), "PLANK")
    assert r["holdSeconds"] == pytest.approx(5, abs=0.2)
    assert r["overall"] == "GOOD"
    assert "plank" in r["summary"]


def test_plank_sag_warning():
    r = analyze(frames_from([plank(sag=30)] * (4 * FPS)), "PLANK")
    assert r["holdSeconds"] > 3
    assert any("sag" in i["text"] for i in r["items"] if i["type"] == "WARNING")


def test_dead_hang_timer():
    hang = kp(dict(nose=(500, 150), shoulder=(500, 180), elbow=(502, 140), wrist=(500, 100),
                   hip=(500, 260), knee=(500, 310), ankle=(500, 360)))
    r = analyze(frames_from([standing()] * 6 + [hang] * (3 * FPS)), "DEAD_HANG")
    assert r["holdSeconds"] == pytest.approx(3, abs=0.2)


# ── failure statuses (FR-020) ──────────────────────────────────────────

def test_nobody_in_frame():
    r = analyze([Frame(t=i / FPS, kpts=None) for i in range(30)], "PUSH_UP", brightness=120)
    assert r["status"] == "FAILED" and r["failureReason"] == "DETECTION_FAILED"


def test_dark_video():
    r = analyze([Frame(t=i / FPS, kpts=None) for i in range(30)], "PUSH_UP", brightness=20)
    assert r["failureReason"] == "POOR_LIGHTING"


def test_occluded_joints():
    poses = []
    for a in rep_angles(3, 170, 80):
        k = pushup(a)
        k[16] = (k[16][0], k[16][1], 0.1)   # ankles hidden
        k[15] = (0, 0, 0.1)
        poses.append(k)
    r = analyze(frames_from(poses), "PUSH_UP")
    assert r["failureReason"] == "OCCLUSION"


def test_unknown_exercise():
    with pytest.raises(ValueError):
        analyze([], "MUSCLE_UP")
