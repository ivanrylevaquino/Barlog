package com.barlog.barlog_back.auth;

import com.barlog.barlog_back.security.GoogleTokenVerifierService;
import com.barlog.barlog_back.security.GoogleTokenVerifierService.GoogleUserInfo;
import com.barlog.barlog_back.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RevokedTokenRepository revokedTokenRepository;
    @Mock private GoogleTokenVerifierService googleVerifierService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                revokedTokenRepository,
                googleVerifierService,
                jwtTokenProvider,
                passwordResetTokenRepository,
                passwordEncoder);
    }

    @Test
    void firstLogin_createsNewUser() {
        String idToken = "valid-token";
        GoogleUserInfo info = new GoogleUserInfo("g-123", "new@barlog.com", "New User", "https://pic.url");
        when(googleVerifierService.verify(idToken)).thenReturn(info);
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.authenticateGoogle(idToken);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("new@barlog.com", response.getUser().getEmail());
        assertEquals(Role.PRACTITIONER, response.getUser().getRole());
        assertEquals(PreferredUnit.KG, response.getUser().getPreferredUnit());

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertEquals("g-123", cap.getValue().getGoogleId());
        assertNotNull(cap.getValue().getCreatedAt());
    }

    @Test
    void returningLogin_noDuplicate() {
        String idToken = "valid-token";
        GoogleUserInfo info = new GoogleUserInfo("g-123", "existing@barlog.com", "Updated", "https://new.pic");
        UUID existingId = UUID.randomUUID();
        User existing = User.builder()
                .userId(existingId).googleId("g-123").email("existing@barlog.com")
                .displayName("Old").role(Role.PRACTITIONER).preferredUnit(PreferredUnit.KG)
                .createdAt(Instant.now().minusSeconds(86400)).build();

        when(googleVerifierService.verify(idToken)).thenReturn(info);
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt");

        AuthResponse response = authService.authenticateGoogle(idToken);

        assertEquals(existingId, response.getUser().getUserId());
        assertEquals("Updated", response.getUser().getDisplayName());
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    void logout_revokesToken() {
        when(jwtTokenProvider.getJtiFromToken("tok")).thenReturn("jti-abc");
        when(jwtTokenProvider.getExpirationFromToken("tok")).thenReturn(Instant.now().plusSeconds(3600));

        authService.logout("Bearer tok");

        ArgumentCaptor<RevokedToken> cap = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(cap.capture());
        assertEquals("jti-abc", cap.getValue().getJti());
    }

    @Test
    void updatePreferences_togglesToLb() {
        UUID uid = UUID.randomUUID();
        User user = User.builder().userId(uid).email("x@y.com").role(Role.PRACTITIONER).preferredUnit(PreferredUnit.KG).build();
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = authService.updatePreferences(uid, PreferredUnit.LB);
        assertEquals(PreferredUnit.LB, result.getPreferredUnit());
    }
}

