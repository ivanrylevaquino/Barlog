package com.barlog.barlog_back.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security principal populated from the JWT claims.
 * Passwordless by design (FR-001 / NFR-004): getPassword() returns "".
 */
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        String grantedRole = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(grantedRole));
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
