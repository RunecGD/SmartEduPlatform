package org.example.core.config;
import org.example.core.filter.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
@Configuration
public class JwtFilterRegistrationConfig {
    @Bean public FilterRegistrationBean<JwtAuthFilter> jwtFilterServletRegistration(JwtAuthFilter filter) {
        var registration=new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
