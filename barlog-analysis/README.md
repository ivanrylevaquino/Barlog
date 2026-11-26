# BarLog pose-analysis service

Python microservice that reads an uploaded exercise video, runs a **pretrained** pose model on it, and returns
rep count / hold time and plain-English form feedback (SRS FR-015 – FR-020). No model training is involved.

## Credits
- **Pose model:** [Ultralytics YOLO pose](https://docs.ultralytics.com/tasks/pose/) (`yolo11n-pose.pt`, pretrained on
  COCO keypoints), AGPL-3.0. Weights download automatically on first run.
- **Rep counting approach:** adapted from Ultralytics' [AIGym workout monitor](https://docs.ultralytics.com/guides/workouts-monitoring)
  (angle at a joint moving between an "up" and a "down" threshold), re-implemented in `barlog_analysis/rules.py`
  to work offline on whole clips, per exercise, with structured JSON output.

## How it works
1. `pose.py` samples the video at ~12 fps and keeps the keypoints of the most prominent person.
2. `rules.py` picks the body side facing the camera, smooths joint angles, then:
   - **Reps** (PUSH_UP, PULL_UP, SQUAT, DIP): counts a rep each time the driving angle goes from extended to flexed;
     partial reps are reported but not counted.
   - **Holds** (PLANK, DEAD_HANG, L_SIT): times the longest stretch spent in position (0.6 s gaps tolerated).
   - **Form**: per-rep checks (depth, body line, lockout, chin over bar, swing, torso lean) become POSITIVE /
     WARNING / ERROR feedback items with timestamps.
   - **Failures**: `DETECTION_FAILED`, `POOR_LIGHTING`, `OCCLUSION` when the person can't be tracked.

3. `detect.py` guesses the exercise when the backend sends `"exercise": "AUTO"` (horizontal body -> push-up/plank,
   hanging -> pull-up/dead hang, knees moving -> squat, ...). The app lets the user correct it.
4. The response also carries `repEvents` (time of each counted and half rep), `partialReps`, `holdWindow` and a
   compact per-frame keypoint `track`, which the web app draws as a skeleton over the video.

Best results: film from the side, whole body in frame, good light. Thresholds live at the top of each exercise in
`rules.py` and are meant to be tuned against real clips.

## Run
```powershell
cd barlog-analysis
python -m venv .venv
.venv\Scripts\pip install --index-url https://download.pytorch.org/whl/cpu torch torchvision
.venv\Scripts\pip install -r requirements.txt
.venv\Scripts\uvicorn barlog_analysis.app:app --port 8001
```
`GET /health`, `POST /analyze {"videoPath": "C:/.../clip.mp4", "exercise": "PUSH_UP"}`.

Tests (synthetic skeletons, no model needed): `.venv\Scripts\python -m pytest -q tests`
