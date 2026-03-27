package com.example.cake.config;

import com.example.cake.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})   // bật CORS default

                // ▬▬▬▬▬▬▬ PERMIT ALL ROUTES ▬▬▬▬▬▬▬
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",        // login/register
                                "/api/courses/**",     // public course
                                "/api/curriculum/**",  // xem curriculum (chapters + lessons)
                                "/api/payment/payos/**", // PayOS webhook
                                "/static/**",          // ảnh courses, avatars
                                "/api/lessons/**"
                        ).permitAll()

                        // admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // tất cả API còn lại cần login
                        .anyRequest().authenticated()
                )

                // ▬▬▬▬▬▬▬ SECURITY STATE ▬▬▬▬▬▬▬
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ▬▬▬▬▬▬▬ JWT FILTER ▬▬▬▬▬▬▬
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // encoder cho password
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager dùng cho login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
