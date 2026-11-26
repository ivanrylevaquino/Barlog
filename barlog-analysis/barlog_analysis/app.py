"""BarLog pose-analysis microservice (SRS FR-015..FR-020).

POST /analyze {"videoPath": "...", "exercise": "PUSH_UP"} -> rep count / hold time / form feedback.
The Spring Boot API calls this asynchronously after a video upload; failures here never break workout logging.
"""
from __future__ import annotations

import logging
import os
import time

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from . import pose
from .rules import SUPPORTED, analyze

log = logging.getLogger("barlog.analysis")
app = FastAPI(title="BarLog pose analysis", version="0.1.0")


class AnalyzeRequest(BaseModel):
    videoPath: str
    exercise: str


@app.get("/health")
def health():
    return {"status": "ok", "model": pose.MODEL_NAME, "exercises": SUPPORTED}


@app.post("/analyze")
def analyze_video(req: AnalyzeRequest):
    if req.exercise not in SUPPORTED:
        raise HTTPException(400, f"Unsupported exercise '{req.exercise}'.")
    if not os.path.isfile(req.videoPath):
        raise HTTPException(404, "Video file not found.")
    started = time.time()
    try:
        frames, fps, brightness = pose.extract(req.videoPath)
    except ValueError as e:
        return {"status": "FAILED", "failureReason": "DETECTION_FAILED", "overall": "FAILED", "repsDetected": 0,
                "holdSeconds": None, "summary": str(e), "items": [], "metrics": {}}
    result = analyze(frames, req.exercise, brightness)
    result["metrics"].update(videoFps=round(fps, 2), brightness=round(brightness, 1),
                             seconds=round(time.time() - started, 2), model=pose.MODEL_NAME)
    log.info("Analyzed %s (%s): %s", req.videoPath, req.exercise, result["summary"])
    return result
