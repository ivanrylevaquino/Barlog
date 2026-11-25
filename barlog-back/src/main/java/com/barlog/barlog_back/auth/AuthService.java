package com.barlog.barlog_back.auth;

import com.barlog.barlog_back.security.GoogleTokenVerifierService;
import com.barlog.barlog_back.security.GoogleTokenVerifierService.GoogleUserInfo;
import com.barlog.barlog_back.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RevokedTokenRepository revokedTokenRepository,
                       GoogleTokenVerifierService googleTokenVerifierService,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse loginWithPassword(PasswordLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, UserDto.from(user));
    }

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

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + request.getEmail()));
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        // TODO: send email with token
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthResourceNotFoundException("Reset token not found"));
        if (resetToken.isExpired()) {
            throw new IllegalStateException("Reset token expired");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }

    @Transactional
    public AuthResponse changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        if (StringUtils.hasText(user.getPasswordHash())) {
            if (!StringUtils.hasText(currentPassword)
                    || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new BadCredentialsException("Current password is incorrect");
            }
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, UserDto.from(user));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String displayName, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        if (displayName != null) user.setDisplayName(displayName);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto updatePreferences(UUID userId, PreferredUnit unit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        user.setPreferredUnit(unit);
        log.info("Updated preferredUnit to {} for user {}", unit, userId);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        return UserDto.from(user);
    }

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
            } catch (Exception ignored) {}
        }
    }
}
