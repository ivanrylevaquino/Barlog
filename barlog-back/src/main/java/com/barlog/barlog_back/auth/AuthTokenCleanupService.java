package com.barlog.barlog_back.auth;

import com.barlog.barlog_back.auth.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Hourly cleanup of expired revoked-token rows to keep the table small. */
@Service
public class AuthTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenCleanupService.class);
    private final RevokedTokenRepository revokedTokenRepository;

    public AuthTokenCleanupService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void purgeExpiredTokens() {
        try {
            int deleted = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
            if (deleted > 0) log.info("Purged {} expired revoked-token rows", deleted);
        } catch (Exception e) {
            log.error("Error purging expired revoked tokens: {}", e.getMessage(), e);
        }
    }
}

