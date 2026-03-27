package com.example.cake.payment.controller;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.payment.model.Payment;
import com.example.cake.payment.service.PaymentService;
import com.example.cake.response.ResponseMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        String ipAddress = getClientIp(httpRequest);

        log.info("Create payment request from user: {} (email: {}), {} courses, IP: {}",
                userId, email, courseIds.size(), ipAddress);

        ResponseMessage<Map<String, String>> response = paymentService.createPayOSPayment(
                userId,
                courseIds,
                orderInfo,
                ipAddress
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
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawBody);

            String signature = root.path("signature").asText(null);
            boolean success = root.path("success").asBoolean(false);
            String topCode = root.path("code").asText(null);

            JsonNode data = root.path("data");
            Long orderCode = data.path("orderCode").isMissingNode() ? null : data.path("orderCode").asLong();
            Long amount = data.path("amount").isMissingNode() ? null : data.path("amount").asLong();
            String dataCode = data.path("code").asText(null);
            String reference = data.path("reference").asText(null);
            String paymentLinkId = data.path("paymentLinkId").asText(null);

            log.info("PayOS webhook received orderCode={} success={} code={} dataCode={} ref={} linkId={} headers={} ",
                    orderCode, success, topCode, dataCode, reference, paymentLinkId, headers.keySet());

            Map<String, String> params = new HashMap<>();
            if (orderCode != null) params.put("orderCode", String.valueOf(orderCode));
            if (amount != null) params.put("amount", String.valueOf(amount));
            if (reference != null) params.put("reference", reference);
            if (paymentLinkId != null) params.put("paymentLinkId", paymentLinkId);
            if (signature != null) params.put("signature", signature);
            params.put("rawBody", rawBody);

            // Normalize status: successful if success==true OR code=="00" OR data.code=="00"
            String normalizedCode = (dataCode != null) ? dataCode : topCode;
            params.put("code", normalizedCode);
            params.put("success", String.valueOf(success));

            ResponseMessage<Map<String, Object>> response = paymentService.processPayOSCallback(params);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("PayOS webhook parse error: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ResponseMessage<>(false, "Webhook payload không hợp lệ", null));
        }
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
