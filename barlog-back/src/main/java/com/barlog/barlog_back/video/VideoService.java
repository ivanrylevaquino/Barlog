package com.barlog.barlog_back.video;

import com.barlog.barlog_back.shared.ResourceNotFoundException;
import com.barlog.barlog_back.workout.ExerciseSet;
import com.barlog.barlog_back.workout.ExerciseSetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

/** Upload, storage and analysis bookkeeping for set videos (FR-011..FR-015, FR-018..FR-020). */
@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    static final Map<String, String> TYPES = Map.of("video/mp4", "mp4", "video/quicktime", "mov", "video/webm", "webm");

    private final VideoClipRepository videos;
    private final ExerciseSetRepository sets;
    private final VideoStorage storage;
    private final VideoUrlSigner signer;
    private final ObjectMapper json;
    private final long maxBytes;

    public VideoService(VideoClipRepository videos, ExerciseSetRepository sets, VideoStorage storage,
                        VideoUrlSigner signer, ObjectMapper json,
                        @Value("${barlog.video.max-mb:100}") long maxMb) {
        this.videos = videos;
        this.sets = sets;
        this.storage = storage;
        this.signer = signer;
        this.json = json;
        this.maxBytes = maxMb * 1024 * 1024;
    }

    /** What the API returns as {@code set.video}. */
    public record VideoDto(UUID id, String url, VideoStatus status, JsonNode analysis, String fileName) {}

    /** Input the analysis runner needs, read in a short transaction. */
    public record AnalysisJob(UUID videoId, String path, String exercise) {}

    // ── Upload ──────────────────────────────────────────────────────────

    @Transactional
    public VideoClip upload(UUID userId, UUID setId, MultipartFile file) {
        ExerciseSet set = sets.findBySetIdAndSession_User_UserId(setId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Set not found."));
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("That file is empty.");
        String ext = TYPES.get(file.getContentType());
        if (ext == null) throw new IllegalArgumentException("Use an MP4, MOV or WebM video.");
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Video is over " + (maxBytes / 1024 / 1024) + " MB. Trim it and try again.");
        }

        videos.findBySet_SetId(setId).ifPresent(old -> {   // one video per set: replace
            storage.delete(old.getStorageKey());
            videos.delete(old);
            videos.flush();
        });

        VideoClip clip = new VideoClip();
        clip.setSet(set);
        clip.setOriginalName(file.getOriginalFilename());
        clip.setContentType(file.getContentType());
        clip.setSizeBytes(file.getSize());
        clip.setStorageKey("pending");
        clip.setStatus(VideoStatus.PROCESSING);
        clip = videos.saveAndFlush(clip);
        clip.setStorageKey(clip.getVideoId() + "." + ext);
        storage.save(file, clip.getStorageKey());
        return clip;
    }

    @Transactional
    public UUID markForReanalysis(UUID userId, UUID videoId) {
        VideoClip clip = videos.findByVideoIdAndSet_Session_User_UserId(videoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found."));
        clip.setStatus(VideoStatus.PROCESSING);
        clip.setFailureReason(null);
        clip.setAnalysisJson(null);
        return clip.getVideoId();
    }

    // ── Analysis bookkeeping ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<AnalysisJob> job(UUID videoId) {
        return videos.findById(videoId).map(c -> new AnalysisJob(c.getVideoId(),
                storage.resolve(c.getStorageKey()).toString(), c.getSet().getExercise().getType().name()));
    }

    @Transactional
    public void recordResult(UUID videoId, String resultJson) {
        videos.findById(videoId).ifPresent(c -> {
            try {
                JsonNode r = json.readTree(resultJson);
                boolean ok = "COMPLETED".equals(r.path("status").asText());
                c.setStatus(ok ? VideoStatus.COMPLETED : VideoStatus.FAILED);
                c.setFailureReason(ok ? null : r.path("failureReason").asText("DETECTION_FAILED"));
                c.setRepsDetected(r.hasNonNull("repsDetected") ? r.get("repsDetected").asInt() : null);
                c.setHoldSeconds(r.hasNonNull("holdSeconds") ? r.get("holdSeconds").asDouble() : null);
                c.setAnalysisJson(resultJson);
            } catch (Exception e) {
                log.warn("Unreadable analysis result for video {}: {}", videoId, e.getMessage());
                fail(c, "DETECTION_FAILED", "The analysis returned an unreadable result.");
            }
            c.setAnalyzedAt(Instant.now());
        });
    }

    /** NFR-010: analysis problems never touch the set itself; the video is kept and tagged. */
    @Transactional
    public void recordServiceFailure(UUID videoId, String reason) {
        videos.findById(videoId).ifPresent(c -> {
            fail(c, "SERVICE_UNAVAILABLE",
                    "Form analysis is offline right now. Your video is saved; try analyzing it again later.");
            c.setAnalyzedAt(Instant.now());
            log.warn("Analysis service failed for video {}: {}", videoId, reason);
        });
    }

    private void fail(VideoClip c, String reason, String summary) {
        ObjectNode n = json.createObjectNode();
        n.put("status", "FAILED").put("overall", "FAILED").put("failureReason", reason).put("summary", summary)
                .put("repsDetected", 0).putArray("items");
        c.setStatus(VideoStatus.FAILED);
        c.setFailureReason(reason);
        c.setAnalysisJson(n.toString());
    }

    // ── Reading ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<UUID, VideoDto> dtosForUser(UUID userId) {
        Map<UUID, VideoDto> out = new HashMap<>();
        for (VideoClip c : videos.findBySet_Session_User_UserId(userId)) out.put(c.getSet().getSetId(), toDto(c));
        return out;
    }

    @Transactional(readOnly = true)
    public VideoDto dtoForSet(UUID setId) {
        return videos.findBySet_SetId(setId).map(this::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<VideoClip> forStreaming(UUID videoId) {
        return videos.findById(videoId);
    }

    VideoDto toDto(VideoClip c) {
        JsonNode analysis = null;
        if (c.getAnalysisJson() != null) {
            try { analysis = json.readTree(c.getAnalysisJson()); } catch (Exception ignored) { }
        }
        return new VideoDto(c.getVideoId(), signer.playbackUrl(c.getVideoId()), c.getStatus(), analysis, c.getOriginalName());
    }

    // ── Clean-up when sets or sessions are deleted ──────────────────────

    @Transactional
    public void deleteForSets(Collection<UUID> setIds) {
        if (setIds.isEmpty()) return;
        for (VideoClip c : videos.findBySet_SetIdIn(setIds)) {
            storage.delete(c.getStorageKey());
            videos.delete(c);
        }
        videos.flush();
    }
}
