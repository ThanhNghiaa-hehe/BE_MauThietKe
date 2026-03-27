package com.example.cake.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
}

