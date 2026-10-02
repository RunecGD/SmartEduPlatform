package com.example.codeexecutionservice.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
public class ExecutionSecurityConfig {
    @Value("${app.cors-origins:http://localhost:4173,http://localhost:5173}") private String origins;
    @Bean public SecurityFilterChain executionSecurity(HttpSecurity http) throws Exception {
        // REST аутентификацию выполняет ExecutionIdentityResolver в обоих методах контроллера.
        http.cors(c->c.configurationSource(corsConfigurationSource())).csrf(c->c.disable())
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.requestMatchers("/api/v1/code/execute","/actuator/health").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
    @Bean public CorsConfigurationSource corsConfigurationSource() {
        var config=new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("POST","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        config.setAllowCredentials(false);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config);
        return source;
    }
}
