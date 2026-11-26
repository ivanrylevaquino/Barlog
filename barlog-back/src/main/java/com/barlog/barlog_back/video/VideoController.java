package com.barlog.barlog_back.video;

import com.barlog.barlog_back.security.UserPrincipal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class VideoController {

    private final VideoService service;
    private final AnalysisRunner runner;
    private final VideoUrlSigner signer;
    private final VideoStorage storage;

    public VideoController(VideoService service, AnalysisRunner runner, VideoUrlSigner signer, VideoStorage storage) {
        this.service = service;
        this.runner = runner;
        this.signer = signer;
        this.storage = storage;
    }

    /** FR-011..FR-015: upload a set video; analysis starts in the background. */
    @PostMapping(path = "/sets/{setId}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoService.VideoDto> upload(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID setId,
                                                        @RequestParam("file") MultipartFile file) {
        VideoClip clip = service.upload(me.getUserId(), setId, file);   // committed when this returns
        runner.run(clip.getVideoId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.toDto(clip));
    }

    /** Try the analysis again, e.g. after the analysis service was offline. */
    @PostMapping("/videos/{id}/reanalyze")
    public ResponseEntity<Void> reanalyze(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        runner.run(service.markForReanalysis(me.getUserId(), id));
        return ResponseEntity.accepted().build();
    }

    /** Signed, expiring playback URL (no JWT header possible from a video tag). Range requests are supported. */
    @GetMapping("/videos/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable UUID id, @RequestParam long exp, @RequestParam String sig) {
        if (!signer.verify(id, exp, sig)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return service.forStreaming(id)
                .map(c -> {
                    Path p = storage.resolve(c.getStorageKey());
                    if (!Files.exists(p)) return ResponseEntity.notFound().<Resource>build();
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(c.getContentType()))
                            .header("Cache-Control", "private, max-age=600")
                            .<Resource>body(new FileSystemResource(p));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
