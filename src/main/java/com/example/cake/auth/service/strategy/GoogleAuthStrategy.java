package com.example.cake.auth.service.strategy;

import com.example.cake.auth.dto.JwtResponse;
import com.example.cake.auth.model.AutheProvider;
import com.example.cake.auth.service.AuthService;
import com.example.cake.response.ResponseMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Strategy: GOOGLE authentication via Firebase idToken.
 */
@Component
@RequiredArgsConstructor
public class GoogleAuthStrategy implements AuthStrategy {

    private final AuthService authService;

    @Override
    public AutheProvider getProvider() {
        return AutheProvider.GOOGLE;
    }

    @Override
    public ResponseMessage<JwtResponse> authenticate(Object request, HttpServletResponse response) {
        String idToken = null;
        if (request instanceof Map<?, ?> map) {
            Object val = map.get("idToken");
            if (val != null) idToken = String.valueOf(val);
        } else if (request instanceof String s) {
            idToken = s;
        }

        if (idToken == null || idToken.isBlank()) {
            return new ResponseMessage<>(false, "Thiếu idToken", null);
        }

        return authService.loginWithGoogle(idToken, response);
    }
}

