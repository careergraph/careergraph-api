package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.config.security.SwaggerAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SwaggerConfig {

    private final SwaggerAuthenticationFilter swaggerAuthenticationFilter;

    public SwaggerConfig(SwaggerAuthenticationFilter swaggerAuthenticationFilter) {
        this.swaggerAuthenticationFilter = swaggerAuthenticationFilter;
    }

    @Bean
    @Order(1) // Higher precedence than the main security config
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Only apply to Swagger UI and OpenAPI endpoints
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            // Disable CSRF for Swagger UI
            .csrf(csrf -> csrf.disable())
            // Add our custom Swagger authentication filter
            .addFilterBefore(swaggerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Allow all requests to be processed by our filter
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
