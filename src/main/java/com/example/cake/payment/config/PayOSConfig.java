package com.example.cake.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "payos")
@Data
public class PayOSConfig {
    private String clientId;
    private String apiKey;
    private String checksumKey;

    private String apiBaseUrl;

    private String returnUrl;
    private String cancelUrl;
    private String webhookUrl;

    /** Base URL of frontend (e.g., http://localhost:5173) used for redirect after PayOS return/cancel */
    private String frontendBaseUrl;

    @PostConstruct
    public void initAutoEnvironment() {
        boolean isProduction = System.getenv("RENDER") != null 
                || System.getenv("RENDER_SERVICE_ID") != null
                || "prod".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE"));

        // If FRONTEND_BASE_URL wasn't explicitly set as env var
        if (System.getenv("FRONTEND_BASE_URL") == null) {
            frontendBaseUrl = isProduction ? "https://fecourse-alpha.vercel.app" : "http://localhost:5173";
        }

        // If PAYOS_RETURN_URL wasn't explicitly set as env var
        if (System.getenv("PAYOS_RETURN_URL") == null) {
            returnUrl = isProduction ? "https://be-mauthietke.onrender.com/api/payment/payos/return" : "http://localhost:8080/api/payment/payos/return";
        }

        // If PAYOS_CANCEL_URL wasn't explicitly set as env var
        if (System.getenv("PAYOS_CANCEL_URL") == null) {
            cancelUrl = isProduction ? "https://be-mauthietke.onrender.com/api/payment/payos/cancel" : "http://localhost:8080/api/payment/payos/cancel";
        }

        // If PAYOS_WEBHOOK_URL wasn't explicitly set as env var
        if (System.getenv("PAYOS_WEBHOOK_URL") == null) {
            webhookUrl = isProduction ? "https://be-mauthietke.onrender.com/api/payment/payos/webhook" : "http://localhost:8080/api/payment/payos/webhook";
        }

        log.info("[PayOSConfig] Auto-configured for environment (isProduction={}): frontendBaseUrl={}, returnUrl={}",
                isProduction, frontendBaseUrl, returnUrl);
    }
}
