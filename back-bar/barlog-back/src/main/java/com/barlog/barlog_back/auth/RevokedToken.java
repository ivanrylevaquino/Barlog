package com.barlog.barlog_back.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Server-side token revocation store.
 * When a user logs out the JWT's jti is inserted here; the JWT filter
 * rejects any token whose jti exists in this table (even if the JWT
 * signature and expiry are still valid).
 * Expired rows are purged hourly by {@link AuthTokenCleanupService}.
 */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(name = "jti", nullable = false, length = 128)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedToken() {}

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}

