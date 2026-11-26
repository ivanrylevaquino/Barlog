"""BarLog pose-analysis microservice (SRS FR-015..FR-020).

POST /analyze {"videoPath": "...", "exercise": "AUTO" | "PUSH_UP" | ..., "hint": "PUSH_UP"}
-> detected exercise, rep count / hold time, form feedback and a skeleton track for playback.
The Spring Boot API calls this asynchronously after a video upload; failures here never break workout logging.
"""
from __future__ import annotations

import logging
import os
import time
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from . import pose
from .detect import detect
from .rules import SUPPORTED, analyze

log = logging.getLogger("barlog.analysis")
app = FastAPI(title="BarLog pose analysis", version="0.2.0")


class AnalyzeRequest(BaseModel):
    videoPath: str
    exercise: str = "AUTO"
    hint: Optional[str] = None


@app.get("/health")
def health():
    return {"status": "ok", "model": pose.MODEL_NAME, "exercises": SUPPORTED}


def track(frames, size) -> dict:
    """Compact keypoint track for drawing the skeleton over the video (ints, 2 decimals for time)."""
    return {"width": size[0], "height": size[1],
            "frames": [{"t": round(f.t, 2),
                        "k": None if f.kpts is None else [[round(x), round(y), round(c, 2)] for x, y, c in f.kpts]}
                       for f in frames]}


@app.post("/analyze")
def analyze_video(req: AnalyzeRequest):
    wanted = req.exercise.upper()
    if wanted != "AUTO" and wanted not in SUPPORTED:
        raise HTTPException(400, f"Unsupported exercise '{req.exercise}'.")
    if not os.path.isfile(req.videoPath):
        raise HTTPException(404, "Video file not found.")
    started = time.time()
    try:
        frames, fps, brightness, size = pose.extract(req.videoPath)
    except ValueError as e:
        return {"status": "FAILED", "failureReason": "DETECTION_FAILED", "overall": "FAILED", "repsDetected": 0,
                "partialReps": 0, "holdSeconds": None, "summary": str(e), "items": [], "repEvents": [], "metrics": {}}

    detected, confidence, features = detect(frames)
    if wanted == "AUTO":
        exercise = detected if detected and confidence >= 0.5 else (req.hint or detected or "PUSH_UP")
    else:
        exercise = wanted
    result = analyze(frames, exercise, brightness)
    result.update(exercise=exercise, detectedExercise=detected, detectionConfidence=round(confidence, 2),
                  track=track(frames, size))
    result["metrics"].update(videoFps=round(fps, 2), brightness=round(brightness, 1), detection=features,
                             seconds=round(time.time() - started, 2), model=pose.MODEL_NAME)
    log.info("Analyzed %s as %s (detected %s @ %.2f): %s", req.videoPath, exercise, detected, confidence, result["summary"])
    return result
