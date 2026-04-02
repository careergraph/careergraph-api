package com.hcmute.careergraph.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

        @Value("${SECURITY_PUBLIC_ENDPOINTS:}")
        private String publicEndpointsRaw;

    @Autowired
    private CorsConfig corsConfig;

    @Autowired
    private JwtDecoder jwtDecoder;

        private String[] resolvePublicEndpoints() {
                return Arrays.stream(publicEndpointsRaw.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toArray(String[]::new);
        }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Base
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // Auth - permitAll MUST come before anyRequest().authenticated()
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(resolvePublicEndpoints()).permitAll()
                        .anyRequest().authenticated())

                // Custom exception
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthenticationEntryPointConfig())
                )

                // OAuth2 - This will attempt JWT decoding for ALL requests with Bearer token
                // But public endpoints already permitted above, so no auth required
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(jwtDecoder)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedConverter = new JwtGrantedAuthoritiesConverter();
        grantedConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedConverter);
        return converter;
    }
}
