package com.example.cake.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Simple request logging filter to help debug which requests produce 401/403.
 * Logs method, path, relevant headers and authentication presence.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String origin = request.getHeader("Origin");
            String auth = request.getHeader("Authorization");

            log.info("[REQ] {} {} Origin={} AuthorizationPresent={}", method, path, origin, auth != null);

            // log a few headers (not sensitive values)
            Enumeration<String> names = request.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = names.nextElement();
                if ("authorization".equalsIgnoreCase(name)) continue; // skip full token
                if ("cookie".equalsIgnoreCase(name)) continue;
                log.debug("[REQ-HEADER] {}: {}", name, request.getHeader(name));
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("[AUTH] authentication present? {}", authentication != null && authentication.isAuthenticated());

        } catch (Exception e) {
            log.warn("Error while logging request: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

