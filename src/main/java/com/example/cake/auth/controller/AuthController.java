package com.example.cake.auth.controller;


import com.example.cake.auth.dto.*;
import com.example.cake.auth.model.User;
import com.example.cake.auth.service.facade.AuthFacade;
import com.example.cake.response.ResponseMessage;
import com.example.cake.user.dto.ForgetPasswordRequest;
import com.example.cake.user.dto.ResetPasswordRequest;
import com.example.cake.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;
    private final UserService userService;


    @PostMapping("/refresh-token")
    public ResponseEntity<ResponseMessage<JwtResponse>> refreshToken(HttpServletRequest request) {
        return ResponseEntity.ok(authFacade.refreshToken(request));
    }

    //login with gg
    @PostMapping("/google")
    public ResponseEntity<ResponseMessage<JwtResponse>> loginGoogle(
            @RequestBody Map<String, String> request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authFacade.loginGoogle(request, response));
    }

    //Đăng ký tạo tài khoản user
    @PostMapping("/register")
    public ResponseEntity<ResponseMessage<Map<String, String>>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authFacade.register(registerRequest));
    }

    //Gửi mã OTP tới email để confirm
    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseMessage<User>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authFacade.verifyOtp(request));
    }

    @PostMapping("/forget-password")
    public ResponseEntity<ResponseMessage<Map<String, String>>> forgetPassword(
            @Valid @RequestBody ForgetPasswordRequest request) {
        return ResponseEntity.ok(userService.sendFogetPassWord(request));
    }

    @PostMapping("/verify-otpPassword")
    public ResponseEntity<ResponseMessage<String>> verifyOtpPassword(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(userService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseMessage<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }
    @PostMapping("/login")
    public ResponseEntity<ResponseMessage<JwtResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authFacade.loginLocal(request, response));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ResponseMessage<Map<String, String>>> resendOtp(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        return ResponseEntity.ok(authFacade.resendOtp(token));
    }
}
