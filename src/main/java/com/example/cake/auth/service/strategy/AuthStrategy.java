package com.example.cake.auth.service.strategy;

import com.example.cake.auth.dto.JwtResponse;
import com.example.cake.auth.model.AutheProvider;
import com.example.cake.response.ResponseMessage;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Strategy interface for authentication mechanisms.
 */
public interface AuthStrategy {

    AutheProvider getProvider();

    /**
     * Authenticate and return access token (and set refresh cookie if needed).
     */
    ResponseMessage<JwtResponse> authenticate(Object request, HttpServletResponse response);
}

