package com.example.cake.auth.service.facade;

import com.example.cake.auth.dto.JwtResponse;
import com.example.cake.auth.dto.LoginRequest;
import com.example.cake.auth.dto.RegisterRequest;
import com.example.cake.auth.dto.VerifyOtpRequest;
import com.example.cake.auth.model.AutheProvider;
import com.example.cake.auth.service.AuthService;
import com.example.cake.auth.service.strategy.AuthStrategy;
import com.example.cake.auth.service.strategy.AuthStrategyFactory;
import com.example.cake.response.ResponseMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Facade: single entry point for Auth flows, keeps controller thin.
 */
@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;
    private final AuthStrategyFactory strategyFactory;

    public ResponseMessage<JwtResponse> loginLocal(LoginRequest request, HttpServletResponse response) {
        AuthStrategy strategy = strategyFactory.getStrategy(AutheProvider.LOCAL);
        return strategy.authenticate(request, response);
    }

    public ResponseMessage<JwtResponse> loginGoogle(Map<String, String> request, HttpServletResponse response) {
        AuthStrategy strategy = strategyFactory.getStrategy(AutheProvider.GOOGLE);
        return strategy.authenticate(request, response);
    }

    public ResponseMessage<Map<String, String>> register(RegisterRequest request) {
        return authService.register(request);
    }

    public ResponseMessage<com.example.cake.auth.model.User> verifyOtp(VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    public ResponseMessage<Map<String, String>> resendOtp(String token) {
        return authService.resendOtp(token);
    }

    public ResponseMessage<JwtResponse> refreshToken(HttpServletRequest request) {
        return authService.refreshTokenFromCookie(request);
    }
}
