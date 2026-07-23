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
    private final com.example.cake.auth.service.EmailService emailService;


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

    //Gửi mã OTP tới email để confirm & tự động đăng nhập
    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> verifyOtp(
            @RequestBody VerifyOtpRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authFacade.verifyOtp(request, response));
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

    // ⚡ Endpoint chẩn đoán gửi mail trên Host - XÓA SAU KHI FIX XONG
    @GetMapping("/test-mail")
    public ResponseEntity<Map<String, Object>> testMail(@RequestParam(defaultValue = "seanpaul1402@gmail.com") String to) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("timestamp", java.time.Instant.now().toString());
        result.put("to", to);
        result.put("provider", "Resend HTTPS API (Port 443)");

        boolean ok = emailService.sendViaResendHttp(to, "[TEST] Resend HTTPS API Test", "Mã OTP thử nghiệm: 123456");
        if (ok) {
            result.put("status", "SUCCESS");
            result.put("message", "Resend HTTPS API sent email successfully!");
        } else {
            result.put("status", "FAILED");
            result.put("message", "Resend HTTPS API failed. Check server logs for details.");
        }

        return ResponseEntity.ok(result);
    }
}
