package br.com.sres;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> values) {
            values.stream().map(Object::toString).forEach(roles::add);
        }
        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            for (Object clientValue : resources.values()) {
                if (clientValue instanceof Map<?, ?> client
                        && client.get("roles") instanceof Collection<?> values) {
                    values.forEach(value -> roles.add(value.toString()));
                }
            }
        }
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .map(GrantedAuthority.class::cast).toList();
    }
}
