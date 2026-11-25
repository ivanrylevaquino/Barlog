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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.barlog.barlog_back.auth.PasswordLoginRequest;


@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RevokedTokenRepository revokedTokenRepository,
                       GoogleTokenVerifierService googleTokenVerifierService,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }


    /** Verify Google ID token, upsert user row, return signed JWT (FR-002, FR-003). */

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

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        return UserDto.from(user);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }

    @Transactional
    public UserDto updatePreferences(UUID userId, PreferredUnit unit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        user.setPreferredUnit(unit);
        log.info("Updated preferredUnit to {} for user {}", unit, userId);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
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

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }
            } catch (Exception e) {
                log.warn("Could not parse token for logout: {}", e.getMessage());
            }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }
        }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        // generate token
        String token = java.util.UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token (out of scope for now)
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Invalid or expired token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthResourceNotFoundException("Expired token");
        }
        User user = prt.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
        // Invalidate token
        passwordResetTokenRepository.delete(prt);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }
}
