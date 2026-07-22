package com.example.cake.auth.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.json.JSONObject;

@Service
public class EmailValidationService {

    @Value("${mailboxlayer.api.key}")
    private String apiKey;

    private final String MAILBOXLAYER_URL = "http://apilayer.net/api/check";

    public boolean isValidEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(MAILBOXLAYER_URL)
                    .queryParam("access_key", apiKey)
                    .queryParam("email", email)
                    .queryParam("smtp", "1")
                    .queryParam("format", "1")
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                System.err.println("⚠️ Email validation API returned empty response");
                return true;
            }

            JSONObject json = new JSONObject(response);

            // Nếu API trả về thông báo lỗi (ví dụ hết quota hoặc key lỗi), fallback cho phép
            if (json.has("success") && !json.optBoolean("success", true)) {
                System.err.println("⚠️ Email validation API error: " + response);
                return true;
            }

            // Kiểm tra format_valid từ API nếu có
            if (json.has("format_valid")) {
                boolean formatValid = json.optBoolean("format_valid", true);
                if (!formatValid) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Email validation API failed: " + e.getMessage());
            return true; // Fallback: nếu API lỗi, vẫn cho phép nếu regex hợp lệ
        }
    }

}
