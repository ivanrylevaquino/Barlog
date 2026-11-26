"""Pretrained pose estimation: video file -> keypoints per sampled frame.

Model: Ultralytics YOLO pose (pretrained on COCO keypoints, no fine-tuning).
https://docs.ultralytics.com/tasks/pose/ — AGPL-3.0. The weights download automatically on first use.
"""
from __future__ import annotations

import os
from functools import lru_cache

import cv2

from .rules import Frame

MODEL_NAME = os.getenv("POSE_MODEL", "yolo11n-pose.pt")
TARGET_FPS = float(os.getenv("POSE_SAMPLE_FPS", "12"))   # frames analysed per second of video
MAX_SECONDS = float(os.getenv("POSE_MAX_SECONDS", "120"))


@lru_cache(maxsize=1)
def model():
    from ultralytics import YOLO
    return YOLO(MODEL_NAME)


def extract(video_path: str) -> tuple[list[Frame], float, float]:
    """Returns (frames, video_fps, mean_brightness)."""
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise ValueError("Could not open the video file.")
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    stride = max(1, round(fps / TARGET_FPS))
    frames: list[Frame] = []
    brightness_sum, brightness_n = 0.0, 0
    i = 0
    try:
        while True:
            ok, img = cap.read()
            if not ok or i / fps > MAX_SECONDS:
                break
            if i % stride == 0:
                gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
                brightness_sum += float(gray.mean())
                brightness_n += 1
                frames.append(Frame(t=i / fps, kpts=_main_person(img)))
            i += 1
    finally:
        cap.release()
    return frames, fps, (brightness_sum / brightness_n if brightness_n else 0.0)


def _main_person(img):
    """Keypoints of the most prominent person (largest box x confidence), or None."""
    res = model().predict(img, verbose=False)[0]
    if res.keypoints is None or res.boxes is None or len(res.boxes) == 0:
        return None
    boxes = res.boxes.xywh.cpu().numpy()
    conf = res.boxes.conf.cpu().numpy()
    best = max(range(len(boxes)), key=lambda k: boxes[k][2] * boxes[k][3] * conf[k])
    xy = res.keypoints.xy.cpu().numpy()[best]
    kc = res.keypoints.conf
    kc = kc.cpu().numpy()[best] if kc is not None else [1.0] * len(xy)
    return [(float(x), float(y), float(c)) for (x, y), c in zip(xy, kc)]
