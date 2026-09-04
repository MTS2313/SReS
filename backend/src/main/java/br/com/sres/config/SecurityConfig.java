package br.com.sres;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakRoleConverter roleConverter, ObjectMapper mapper) throws Exception {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeProblem(response, request, mapper, 401, "Autenticação necessária."))
                        .accessDeniedHandler((request, response, exception) -> writeProblem(response, request, mapper, 403, "Acesso negado.")))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }

    private static void writeProblem(jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request,
                                     ObjectMapper mapper, int status, String detail) throws java.io.IOException {
        var problem = org.springframework.http.ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(status), detail);
        problem.setType(java.net.URI.create("urn:sres:problem:" + status));
        problem.setTitle(status == 401 ? "Unauthorized" : "Forbidden");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        String correlationId = CorrelationIdFilter.current(request);
        if (correlationId != null) problem.setProperty("correlationId", correlationId);
        response.setStatus(status);
        response.setContentType("application/problem+json");
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
