package com.hcmute.careergraph.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String allowedOrigins;

    @Value("${CORS_ALLOWED_METHODS:*}")
    private String allowedMethods;

    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String allowedHeaders;

    private List<String> parseCsv(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        parseCsv(allowedOrigins).forEach(corsConfiguration::addAllowedOrigin);
        parseCsv(allowedMethods).forEach(corsConfiguration::addAllowedMethod);
        parseCsv(allowedHeaders).forEach(corsConfiguration::addAllowedHeader);

        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
