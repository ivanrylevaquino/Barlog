package com.barlog.barlog_back.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Time-limited signed playback URLs, the local equivalent of S3 presigned GET URLs (NFR-006: valid 15 minutes,
 * handed only to the owner). A {@code <video>} tag cannot send the JWT header, so the URL carries its own proof.
 */
@Component
public class VideoUrlSigner {

    static final long TTL_SECONDS = 15 * 60;

    private final byte[] key;
    private final String fallbackBase;

    public VideoUrlSigner(@Value("${barlog.jwt.secret}") String secret,
                          @Value("${barlog.public-base-url:http://localhost:8080}") String fallbackBase) {
        this.key = ("video-url:" + secret).getBytes(StandardCharsets.UTF_8);
        this.fallbackBase = fallbackBase;
    }

    public String playbackUrl(UUID videoId) {
        long exp = Instant.now().getEpochSecond() + TTL_SECONDS;
        String base;
        try {
            base = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        } catch (IllegalStateException noRequest) {
            base = fallbackBase;
        }
        return base + "/api/videos/" + videoId + "/stream?exp=" + exp + "&sig=" + sign(videoId, exp);
    }

    public boolean verify(UUID videoId, long exp, String sig) {
        if (sig == null || exp < Instant.now().getEpochSecond()) return false;
        return MessageDigest.isEqual(sign(videoId, exp).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }

    String sign(UUID videoId, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] h = mac.doFinal((videoId + ":" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
