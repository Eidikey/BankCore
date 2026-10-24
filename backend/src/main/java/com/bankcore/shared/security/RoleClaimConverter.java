package com.bankcore.shared.security;

import com.bankcore.user.domain.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Maps the {@code role} claim (CLIENT, EMPLOYEE, ADMIN) to Spring Security
 * authorities ({@code ROLE_CLIENT}, etc.).
 */
@Component
public class RoleClaimConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    public RoleClaimConverter() {
        delegate.setJwtGrantedAuthoritiesConverter(this::authorities);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return delegate.convert(jwt);
    }

    private Collection<GrantedAuthority> authorities(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role == null || role.isBlank()) {
            return List.of();
        }
        try {
            Role parsed = Role.valueOf(role);
            return List.of(new SimpleGrantedAuthority("ROLE_" + parsed.name()));
        } catch (IllegalArgumentException ex) {
            // Fail closed: a signed token with an unknown role gets no authority.
            return List.of();
        }
    }
}
