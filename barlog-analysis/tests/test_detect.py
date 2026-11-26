import sys, os
sys.path.insert(0, os.path.dirname(__file__))

from test_rules import FPS, dip, frames_from, kp, plank, pullup, pushup, rep_angles, squat  # noqa: E402
from barlog_analysis.detect import detect  # noqa: E402
from barlog_analysis.rules import analyze  # noqa: E402


def test_detects_pushups():
    assert detect(frames_from([pushup(a) for a in rep_angles(3, 170, 80)]))[0] == "PUSH_UP"


def test_detects_plank():
    assert detect(frames_from([plank()] * 40))[0] == "PLANK"


def test_detects_pullups_and_hang():
    assert detect(frames_from([pullup(a) for a in rep_angles(3, 170, 40)]))[0] == "PULL_UP"
    assert detect(frames_from([pullup(170)] * 40))[0] == "DEAD_HANG"


def test_detects_squats():
    assert detect(frames_from([squat(a) for a in rep_angles(3, 175, 60)]))[0] == "SQUAT"


def test_detects_dips():
    assert detect(frames_from([dip(a) for a in rep_angles(3, 170, 85)]))[0] == "DIP"


def test_rep_events_include_partials():
    trace = rep_angles(3, 170, 80) + rep_angles(2, 170, 125, lead_s=0)
    r = analyze(frames_from([pushup(a) for a in trace]), "PUSH_UP")
    assert r["repsDetected"] == 3 and r["partialReps"] == 2
    assert [e["counted"] for e in r["repEvents"]].count(False) == 2
