package com.example.cake.payment.controller;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.payment.config.PayOSConfig;
import com.example.cake.payment.model.Payment;
import com.example.cake.payment.service.PaymentService;
import com.example.cake.payment.service.payos.PayOSWebhookAdapter;
import com.example.cake.response.ResponseMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.example.cake.payment.repository.PaymentRepository;
import com.example.cake.payment.service.event.PaymentSucceededEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Payment Controller - PayOS Integration
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final PayOSConfig payOSConfig;
    private final PayOSWebhookAdapter payOSWebhookAdapter;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create PayOS payment - Direct course purchase
     *
     * @param request Payment request with courseIds
     * @param httpRequest HTTP request to get IP
     * @param authentication User authentication
     * @return Payment URL
     */
    @PostMapping("/payos/create")
    public ResponseEntity<ResponseMessage<Map<String, String>>> createPayOSPayment(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest,
            Authentication authentication
    ) {
        // Get email from authentication (JWT subject)
        String email = authentication != null ? authentication.getName() : null;

        if (email == null) {
            log.error("User not authenticated");
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        // Find user by email to get userId
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.error("User not found with email: {}", email);
            return ResponseEntity.ok(new ResponseMessage<>(false, "Không tìm thấy người dùng", null));
        }

        String userId = user.getId();

        // Get courseIds from request
        @SuppressWarnings("unchecked")
        java.util.List<String> courseIds = (java.util.List<String>) request.get("courseIds");

        if (courseIds == null || courseIds.isEmpty()) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng chọn ít nhất một khóa học", null));
        }

        String orderInfo = (String) request.getOrDefault("orderInfo", "Thanh toan khoa hoc");
        String couponCode = (String) request.get("couponCode");
        String ipAddress = getClientIp(httpRequest);

        log.info("Create payment request from user: {} (email: {}), {} courses, IP: {}, coupon: {}",
                userId, email, courseIds.size(), ipAddress, couponCode);

        ResponseMessage<Map<String, String>> response = paymentService.createPayOSPayment(
                userId,
                courseIds,
                orderInfo,
                ipAddress,
                couponCode
        );

        return ResponseEntity.ok(response);
    }

    /**
     * PayOS webhook callback (server-to-server).
     * Expected schema (example):
     * { code, desc, success, data: { orderCode, amount, ... , code, desc }, signature }
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> payOSWebhook(
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        PayOSWebhookAdapter.AdaptedWebhook adapted = payOSWebhookAdapter.adapt(rawBody, headers);
        if (adapted.getParams() == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false,
                    adapted.getErrorMessage() != null ? adapted.getErrorMessage() : "Webhook payload không hợp lệ",
                    null));
        }

        ResponseMessage<Map<String, Object>> response = paymentService.processPayOSCallback(adapted.getParams());
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment status
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<ResponseMessage<Payment>> getPaymentStatus(
            @PathVariable String paymentId,
            Authentication authentication
    ) {
        ResponseMessage<Payment> response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for current user
     */
    @GetMapping("/my-payments")
    public ResponseEntity<ResponseMessage<java.util.List<Payment>>> getMyPayments(
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Không tìm thấy người dùng", null));
        }

        ResponseMessage<java.util.List<Payment>> response = paymentService.getUserPayments(user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get successful payments for current user
     */
    @GetMapping("/my-payments/success")
    public ResponseEntity<ResponseMessage<java.util.List<Payment>>> getMySuccessfulPayments(
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Không tìm thấy người dùng", null));
        }

        ResponseMessage<java.util.List<Payment>> response = paymentService.getUserSuccessfulPayments(user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * PayOS browser return endpoint (Option B): PayOS redirects here, then BE redirects to FE.
     * This endpoint verifies return status and updates payment DB status + triggers receipt email immediately.
     */
    @GetMapping("/payos/return")
    public ResponseEntity<Void> payOSReturnRedirect(
            @RequestParam Map<String, String> queryParams
    ) {
        String feBase = normalizeFrontendBaseUrl(payOSConfig.getFrontendBaseUrl());

        String orderCodeStr = firstNonBlank(queryParams.get("orderCode"), queryParams.get("order_code"), queryParams.get("order"));
        String code = firstNonBlank(queryParams.get("code"), queryParams.get("responseCode"));
        String status = firstNonBlank(queryParams.get("status"), ("00".equals(code) ? "SUCCESS" : null));
        boolean isSuccess = "00".equals(code) || "SUCCESS".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);

        if (isSuccess && orderCodeStr != null) {
            try {
                Long orderCode = Long.valueOf(orderCodeStr);
                Payment payment = paymentRepository.findByProviderOrderCode(orderCode).orElse(null);
                if (payment != null && payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
                    payment.setStatus(Payment.PaymentStatus.SUCCESS);
                    payment.setPaidAt(java.time.LocalDateTime.now());
                    payment.setProviderResponseCode(code);
                    paymentRepository.save(payment);

                    java.util.List<String> courseIds = payment.getCourses() == null
                            ? java.util.List.of()
                            : payment.getCourses().stream().map(Payment.PaymentCourseItem::getCourseId).toList();

                    eventPublisher.publishEvent(PaymentSucceededEvent.builder()
                            .paymentId(payment.getId())
                            .userId(payment.getUserId())
                            .courseIds(courseIds)
                            .amount(payment.getAmount())
                            .paidAt(payment.getPaidAt())
                            .build());

                    log.info("🎉 [PayOS Return] Processed payment success & triggered receipt email for orderCode={} paymentId={}",
                            orderCode, payment.getId());
                }
            } catch (Exception e) {
                log.error("❌ [PayOS Return] Error updating payment status on return: {}", e.getMessage(), e);
            }
        }

        String redirectUrl = feBase + "/payment/return"
                + "?status=" + url(isSuccess ? "SUCCESS" : (status != null ? status : "PENDING"))
                + "&code=" + url(code != null ? code : "")
                + "&cancel=false"
                + (orderCodeStr != null ? "&orderCode=" + url(orderCodeStr) : "");

        log.info("[PayOS] return redirect -> {} (from query={})", redirectUrl, queryParams);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * PayOS browser cancel endpoint (Option B): PayOS redirects here, then BE redirects to FE.
     */
    @GetMapping("/payos/cancel")
    public ResponseEntity<Void> payOSCancelRedirect(
            @RequestParam Map<String, String> queryParams
    ) {
        String feBase = normalizeFrontendBaseUrl(payOSConfig.getFrontendBaseUrl());

        String orderCodeStr = firstNonBlank(queryParams.get("orderCode"), queryParams.get("order_code"), queryParams.get("order"));
        String code = firstNonBlank(queryParams.get("code"), queryParams.get("responseCode"));

        if (orderCodeStr != null) {
            try {
                Long orderCode = Long.valueOf(orderCodeStr);
                Payment payment = paymentRepository.findByProviderOrderCode(orderCode).orElse(null);
                if (payment != null && payment.getStatus() == Payment.PaymentStatus.PENDING) {
                    payment.setStatus(Payment.PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                    log.info("⚠️ [PayOS Cancel] Payment marked CANCELLED for orderCode={}", orderCode);
                }
            } catch (Exception e) {
                log.error("❌ [PayOS Cancel] Error updating payment status on cancel: {}", e.getMessage(), e);
            }
        }

        String redirectUrl = feBase + "/payment/cancel"
                + "?status=" + url("CANCELLED")
                + "&code=" + url(code != null ? code : "")
                + "&cancel=true"
                + (orderCodeStr != null ? "&orderCode=" + url(orderCodeStr) : "");

        log.info("[PayOS] cancel redirect -> {} (from query={})", redirectUrl, queryParams);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private static String normalizeFrontendBaseUrl(String feBase) {
        if (feBase == null || feBase.isBlank()) return "http://localhost:5173";
        // Remove trailing slash
        if (feBase.endsWith("/")) return feBase.substring(0, feBase.length() - 1);
        return feBase;
    }

    private static String url(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        // Convert IPv6 localhost to IPv4
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        return ipAddress;
    }
}
