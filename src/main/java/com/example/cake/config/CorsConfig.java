package com.example.cake.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS config via MVC - disabled in favor of Spring Security CORS config.
 * Spring Security's corsConfigurationSource() in SecurityConfig takes precedence.
 */
@Configuration
public class CorsConfig {

    // Disabled: CORS is now configured in SecurityConfig.corsConfigurationSource()
    // Having two CORS sources can cause conflicts, especially with credentials=true
    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        ...
    }
    */
}
