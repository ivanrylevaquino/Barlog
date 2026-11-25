package com.barlog.barlog_back.security;

import com.barlog.barlog_back.auth.PreferredUnit;
import com.barlog.barlog_back.auth.Role;
import com.barlog.barlog_back.auth.RevokedTokenRepository;
import com.barlog.barlog_back.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private JwtTokenProvider provider;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 86_400_000L, revokedTokenRepository);
    }

    @Test
    void generateAndValidate_success() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .googleId("g-12345")
                .email("athlete@barlog.com")
                .displayName("Athlete")
                .role(Role.PRACTITIONER)
                .preferredUnit(PreferredUnit.KG)
                .build();

        when(revokedTokenRepository.existsById(anyString())).thenReturn(false);

        String token = provider.generateToken(user);
        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals(user.getUserId(), provider.getUserIdFromToken(token));
        assertEquals("ROLE_PRACTITIONER", provider.getRoleFromToken(token));
        assertEquals("athlete@barlog.com", provider.getEmailFromToken(token));
        assertNotNull(provider.getJtiFromToken(token));
    }

    @Test
    void validate_revokedToken_returnsFalse() {
        User user = User.builder()
                .userId(UUID.randomUUID()).email("x@y.com").role(Role.PRACTITIONER).build();
        String token = provider.generateToken(user);
        String jti = provider.getJtiFromToken(token);
        when(revokedTokenRepository.existsById(jti)).thenReturn(true);
        assertFalse(provider.validateToken(token));
    }

    @Test
    void validate_wrongSignature_returnsFalse() {
        JwtTokenProvider other = new JwtTokenProvider(
                "AnotherSecretKeyThatIs32BytesLongABC12345678XX", 86_400_000L, revokedTokenRepository);
        User user = User.builder()
                .userId(UUID.randomUUID()).email("x@y.com").role(Role.PRACTITIONER).build();
        String token = other.generateToken(user);
        assertFalse(provider.validateToken(token));
    }
}
