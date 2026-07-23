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
    private final org.springframework.mail.javamail.JavaMailSender mailSender;


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

        try {
            org.springframework.mail.javamail.JavaMailSenderImpl senderImpl =
                    (org.springframework.mail.javamail.JavaMailSenderImpl) mailSender;
            result.put("host", senderImpl.getHost());
            result.put("port", senderImpl.getPort());
            result.put("username", senderImpl.getUsername());
            result.put("passwordLength", senderImpl.getPassword() != null ? senderImpl.getPassword().length() : 0);
            result.put("javaMailProperties", senderImpl.getJavaMailProperties().toString());
        } catch (Exception e) {
            result.put("configError", e.getMessage());
        }

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom("seanpaul1402@gmail.com", "CodeLearn Test");
            helper.setTo(to);
            helper.setSubject("[TEST] Diagnostic email from Render Host");
            helper.setText("If you receive this, email sending works on Render Host!");

            mailSender.send(mimeMessage);
            result.put("status", "SUCCESS");
            result.put("message", "Email sent successfully!");
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getClass().getName());
            result.put("message", e.getMessage());
            if (e.getCause() != null) {
                result.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
        }

        return ResponseEntity.ok(result);
    }
}
