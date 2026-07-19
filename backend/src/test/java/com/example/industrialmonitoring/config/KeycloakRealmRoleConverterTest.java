package com.example.industrialmonitoring.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter =
            new KeycloakRealmRoleConverter();

    @Test
    void shouldConvertRealmRolesToSpringAuthorities() {
        Jwt jwt = createJwt(
                Map.of(
                        "realm_access",
                        Map.of(
                                "roles",
                                List.of("VIEWER", "OPERATOR")
                        )
                )
        );

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_VIEWER",
                        "ROLE_OPERATOR"
                );
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRealmAccessIsMissing() {
        Jwt jwt = createJwt(
                Map.of(
                        "sub",
                        "monitoring-viewer"
                )
        );

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesAreMissing() {
        Jwt jwt = createJwt(
                Map.of(
                        "realm_access",
                        Map.of()
                )
        );

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );
    }
}