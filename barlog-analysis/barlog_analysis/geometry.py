"""Small geometry helpers over COCO-17 keypoints (x, y, confidence), image y grows downward."""
from __future__ import annotations

import math
from typing import Optional, Sequence

# COCO-17 keypoint indices, as produced by Ultralytics YOLO pose models.
NOSE = 0
L_SHOULDER, R_SHOULDER = 5, 6
L_ELBOW, R_ELBOW = 7, 8
L_WRIST, R_WRIST = 9, 10
L_HIP, R_HIP = 11, 12
L_KNEE, R_KNEE = 13, 14
L_ANKLE, R_ANKLE = 15, 16

SIDES = {
    "left": dict(shoulder=L_SHOULDER, elbow=L_ELBOW, wrist=L_WRIST, hip=L_HIP, knee=L_KNEE, ankle=L_ANKLE),
    "right": dict(shoulder=R_SHOULDER, elbow=R_ELBOW, wrist=R_WRIST, hip=R_HIP, knee=R_KNEE, ankle=R_ANKLE),
}

Point = Sequence[float]  # (x, y, conf)


def angle(a: Point, b: Point, c: Point) -> float:
    """Angle ABC in degrees (0..180)."""
    v1 = (a[0] - b[0], a[1] - b[1])
    v2 = (c[0] - b[0], c[1] - b[1])
    n1, n2 = math.hypot(*v1), math.hypot(*v2)
    if n1 == 0 or n2 == 0:
        return float("nan")
    cos = max(-1.0, min(1.0, (v1[0] * v2[0] + v1[1] * v2[1]) / (n1 * n2)))
    return math.degrees(math.acos(cos))


def tilt_from_vertical(top: Point, bottom: Point) -> float:
    """How far the segment bottom->top leans away from straight up, in degrees (0 = upright)."""
    dx, dy = top[0] - bottom[0], bottom[1] - top[1]
    return math.degrees(math.atan2(abs(dx), dy)) if (dx or dy) else float("nan")


def tilt_from_horizontal(a: Point, b: Point) -> float:
    dx, dy = abs(b[0] - a[0]), abs(b[1] - a[1])
    return math.degrees(math.atan2(dy, dx)) if (dx or dy) else float("nan")


def signed_offset_from_line(p: Point, a: Point, b: Point) -> float:
    """Perpendicular distance of p from line a->b, positive when p is *below* the line in the image."""
    ax, ay, bx, by = a[0], a[1], b[0], b[1]
    length = math.hypot(bx - ax, by - ay)
    if length == 0:
        return 0.0
    cross = (bx - ax) * (p[1] - ay) - (by - ay) * (p[0] - ax)
    d = cross / length
    # Make "below" positive regardless of which way the line points.
    return d if bx >= ax else -d


def dist(a: Point, b: Point) -> float:
    return math.hypot(a[0] - b[0], a[1] - b[1])


def smooth(values: list[Optional[float]], window: int = 5) -> list[Optional[float]]:
    """Moving median that skips missing values; keeps None where no neighbour has data."""
    out: list[Optional[float]] = []
    half = window // 2
    for i in range(len(values)):
        neigh = sorted(v for v in values[max(0, i - half): i + half + 1]
                       if v is not None and not math.isnan(v))
        out.append(neigh[len(neigh) // 2] if neigh else None)
    return out


def fmt_time(seconds: float) -> str:
    s = max(0, int(round(seconds)))
    return f"{s // 60}:{s % 60:02d}"
