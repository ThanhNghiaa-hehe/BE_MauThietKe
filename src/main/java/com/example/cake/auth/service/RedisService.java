package com.example.cake.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void saveOtp(String token, String jsonData, long timeoutMinutes) {
        try {
            redisTemplate.opsForValue().set("otp_token:" + token, jsonData, timeoutMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis saveOtp failed: " + e.getMessage());
        }
    }

    public String getOtp(String token) {
        try {
            return redisTemplate.opsForValue().get("otp_token:" + token);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis getOtp failed: " + e.getMessage());
            return null;
        }
    }

    public void deleteOtp(String token) {
        try {
            redisTemplate.delete("otp_token:" + token);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis deleteOtp failed: " + e.getMessage());
        }
    }

    public String getEmailFromToken(String token) {
        try {
            String json = getOtp(token);
            if (json == null) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            return node.get("email").asText();
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis getEmailFromToken failed: " + e.getMessage());
            return null;
        }
    }

    public void saveRefreshToken(String email, String refreshToken, long minutes) {
        try {
            redisTemplate.opsForValue().set("refresh:" + email, refreshToken, Duration.ofMinutes(minutes));
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis saveRefreshToken failed: " + e.getMessage());
        }
    }

    public String getRefreshToken(String email) {
        try {
            return redisTemplate.opsForValue().get("refresh:" + email);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis getRefreshToken failed: " + e.getMessage());
            return null;
        }
    }

    public String getEmailFromRefreshToken(String refreshToken) {
        try {
            Set<String> keys = redisTemplate.keys("refresh:*");
            if (keys != null) {
                for (String key : keys) {
                    String value = redisTemplate.opsForValue().get(key);
                    if (refreshToken.equals(value)) {
                        return key.replace("refresh:", "");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis getEmailFromRefreshToken failed: " + e.getMessage());
        }
        return null;
    }

    public void deleteRefreshToken(String email) {
        try {
            redisTemplate.delete("refresh:" + email);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Redis deleteRefreshToken failed: " + e.getMessage());
        }
    }
}
