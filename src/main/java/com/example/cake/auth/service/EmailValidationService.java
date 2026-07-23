package com.example.cake.auth.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class EmailValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Fast local regex validation (0.01ms) - avoids slow external HTTP API calls.
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
