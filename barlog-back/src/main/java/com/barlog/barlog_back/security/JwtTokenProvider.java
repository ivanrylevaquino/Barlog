package com.barlog.barlog_back.security;

import com.barlog.barlog_back.auth.RevokedTokenRepository;
import com.barlog.barlog_back.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HMAC-SHA256 (HS256) JWTs.
 * Max validity is capped at 24 h per SRS FR-003.
 * Revoked JTIs (from logout) are rejected via {@link RevokedTokenRepository}.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtTokenProvider(
            @Value("${barlog.jwt.secret}") String secret,
            @Value("${barlog.jwt.expiration-ms:86400000}") long expirationMs,
            RevokedTokenRepository revokedTokenRepository) {
        this.expirationMs = Math.min(expirationMs, 86_400_000L);   // cap at 24 h
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUserId().toString())
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("role", "ROLE_" + user.getRole().name())
                .claim("name", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            String jti = claims.getId();
            if (jti != null && revokedTokenRepository.existsById(jti)) {
                log.warn("Rejected revoked token jti={}", jti);
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                   .parseSignedClaims(token).getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getJtiFromToken(String token) {
        return getClaims(token).getId();
    }

    public Instant getExpirationFromToken(String token) {
        return getClaims(token).getExpiration().toInstant();
    }
}
