package com.example.industrialmonitoring.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KeycloakRealmRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return Collections.emptyList();
        }

        Object rolesClaim = realmAccess.get("roles");

        if (!(rolesClaim instanceof Collection<?>)) {
            return Collections.emptyList();
        }

        Collection<?> roles = (Collection<?>) rolesClaim;
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Object role : roles) {
            if (role instanceof String) {
                authorities.add(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );
            }
        }

        return authorities;
    }
}