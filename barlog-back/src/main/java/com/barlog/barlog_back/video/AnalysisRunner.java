package com.barlog.barlog_back.video;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Runs pose analysis off the request thread (FR-015 asynchronous dispatch). */
@Component
public class AnalysisRunner {

    private final VideoService videos;
    private final AnalysisClient client;

    public AnalysisRunner(VideoService videos, AnalysisClient client) {
        this.videos = videos;
        this.client = client;
    }

    @Async
    public void run(UUID videoId) {
        videos.job(videoId).ifPresent(job -> {
            String result;
            try {
                result = client.analyze(job.path(), "AUTO", job.exercise());
            } catch (Exception e) {
                videos.recordServiceFailure(videoId, e.getMessage());
                return;
            }
            videos.recordResult(videoId, result);
        });
    }
}
