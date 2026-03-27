package com.example.cake.auth.service.strategy;

import com.example.cake.auth.dto.JwtResponse;
import com.example.cake.auth.dto.LoginRequest;
import com.example.cake.auth.model.AutheProvider;
import com.example.cake.auth.service.AuthService;
import com.example.cake.response.ResponseMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Strategy: LOCAL (email/password) authentication.
 */
@Component
@RequiredArgsConstructor
public class LocalAuthStrategy implements AuthStrategy {

    private final AuthService authService;

    @Override
    public AutheProvider getProvider() {
        return AutheProvider.LOCAL;
    }

    @Override
    public ResponseMessage<JwtResponse> authenticate(Object request, HttpServletResponse response) {
        if (!(request instanceof LoginRequest loginRequest)) {
            return new ResponseMessage<>(false, "Sai định dạng dữ liệu đăng nhập", null);
        }
        return authService.login(loginRequest, response);
    }
}

