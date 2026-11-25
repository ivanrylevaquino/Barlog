package com.barlog.barlog_back.auth;

import com.barlog.barlog_back.security.GoogleTokenVerifierService;
import com.barlog.barlog_back.security.GoogleTokenVerifierService.GoogleUserInfo;
import com.barlog.barlog_back.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RevokedTokenRepository revokedTokenRepository,
                       GoogleTokenVerifierService googleTokenVerifierService,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** Verify Google ID token, upsert user row, return signed JWT (FR-002, FR-003). */
    @Transactional
    public AuthResponse authenticateGoogle(String idToken) {
        GoogleUserInfo googleUser = googleTokenVerifierService.verify(idToken);
        Instant now = Instant.now();

        Optional<User> existing = userRepository.findByGoogleId(googleUser.getGoogleId());
        User user;

        if (existing.isPresent()) {
            user = existing.get();
            user.setLastLoginAt(now);
            if (StringUtils.hasText(googleUser.getDisplayName())) user.setDisplayName(googleUser.getDisplayName());
            if (StringUtils.hasText(googleUser.getAvatarUrl())) user.setAvatarUrl(googleUser.getAvatarUrl());
            user = userRepository.save(user);
            log.info("Returning user authenticated: {} ({})", user.getEmail(), user.getUserId());
        } else {
            user = User.builder()
                    .googleId(googleUser.getGoogleId())
                    .email(googleUser.getEmail())
                    .displayName(googleUser.getDisplayName())
                    .avatarUrl(googleUser.getAvatarUrl())
                    .role(Role.PRACTITIONER)
                    .preferredUnit(PreferredUnit.KG)
                    .createdAt(now)
                    .lastLoginAt(now)
                    .build();
            user = userRepository.save(user);
            log.info("New practitioner registered: {} ({})", user.getEmail(), user.getUserId());
        }

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updatePreferences(UUID userId, PreferredUnit unit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        user.setPreferredUnit(unit);
        log.info("Updated preferredUnit to {} for user {}", unit, userId);
        return UserDto.from(userRepository.save(user));
    }

    /** Revoke the JWT server-side so it cannot be reused (FR-004 logout). */
    @Transactional
    public void logout(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                String jti = jwtTokenProvider.getJtiFromToken(token);
                Instant expiresAt = jwtTokenProvider.getExpirationFromToken(token);
                if (StringUtils.hasText(jti)) {
                    revokedTokenRepository.save(new RevokedToken(jti, expiresAt));
                    log.info("Revoked token jti={}", jti);
                }
            } catch (Exception e) {
                log.warn("Could not parse token for logout: {}", e.getMessage());
            }
        }
        SecurityContextHolder.clearContext();
    }
}
