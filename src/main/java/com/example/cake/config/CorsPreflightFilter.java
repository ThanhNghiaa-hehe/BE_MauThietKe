package com.example.cake.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handle CORS preflight (OPTIONS) requests early in the filter chain.
 * Ensures OPTIONS requests return 200 immediately without going through security filters.
 * Runs with HIGHEST_PRECEDENCE to execute before other filters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorsPreflightFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            String origin = httpRequest.getHeader("Origin");
            String requestMethod = httpRequest.getHeader("Access-Control-Request-Method");
            String requestHeaders = httpRequest.getHeader("Access-Control-Request-Headers");

            log.info("[CORS-PREFLIGHT] Origin={} Method={} Headers={}", origin, requestMethod, requestHeaders);

            // Allow all dev origins
            if (origin != null && (
                    origin.contains("localhost") || 
                    origin.contains("127.0.0.1") ||
                    origin.contains("192.168")
            )) {
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
                httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                httpResponse.setHeader("Access-Control-Max-Age", "3600");
                httpResponse.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}


