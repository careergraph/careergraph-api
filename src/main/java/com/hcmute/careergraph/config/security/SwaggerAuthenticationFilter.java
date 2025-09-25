package com.hcmute.careergraph.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

@Component
public class SwaggerAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerAuthenticationFilter.class);

    @Value("${swagger.username:admin}")
    private String swaggerUsername;

    @Value("${swagger.password:password}")
    private String swaggerPassword;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Only apply to Swagger UI and OpenAPI endpoints
        if (isSwaggerUIRequest(requestURI)) {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Basic ")) {
                // Extract credentials
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String username = values[0];
                String password = values[1];

                // Validate credentials
                if (swaggerUsername.equals(username) && swaggerPassword.equals(password)) {
                    // Set authentication in context
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_SWAGGER_ADMIN")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    LOG.debug("Authenticated user '{}' for Swagger UI", username);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // If no valid credentials, return 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Swagger UI\"");
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized: Swagger UI requires authentication\"}");
            return;
        }

        // For non-Swagger requests, continue the filter chain
        filterChain.doFilter(request, response);
    }

    private boolean isSwaggerUIRequest(String uri) {
        return uri.startsWith("/swagger-ui/") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs/") ||
               uri.startsWith("/v3/api-docs");
    }
}
