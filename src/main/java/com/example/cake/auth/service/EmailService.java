package com.example.cake.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    public boolean sendViaResendHttp(String to, String subject, String contentText) {
        try {
            if (resendApiKey == null || resendApiKey.isBlank()) {
                log.error("❌ [EmailService] RESEND_API_KEY is not set in environment variables!");
                return false;
            }
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());

            Map<String, Object> body = new HashMap<>();
            body.put("from", "CodeLearn <onboarding@resend.dev>");
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("html", "<div style=\"font-family: Arial, sans-serif; font-size: 16px; color: #333;\">"
                    + contentText.replace("\n", "<br/>") + "</div>");

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("🚀 [EmailService] Resend HTTPS API sent successfully to {}: {}", to, response.getBody());
                return true;
            } else {
                log.error("❌ [EmailService] Resend HTTPS API error {}: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ [EmailService] Resend HTTPS API exception for {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    public void sendOtpEmail(String to, String otp) {
        CompletableFuture.runAsync(() -> {
            String subject = "Mã OTP xác minh - CodeLearn";
            String text = "Mã OTP của bạn là: " + otp + "\nHiệu lực trong 5 phút.";
            sendViaResendHttp(to, subject, text);
        });
    }

    public void sendOtpForgetPassWord(String to, String otp) {
        CompletableFuture.runAsync(() -> {
            String subject = "Mã OTP Khôi phục mật khẩu - CodeLearn";
            String text = "Mã OTP của bạn là: " + otp + "\nHiệu lực trong 5 phút.";
            sendViaResendHttp(to, subject, text);
        });
    }

    public void sendInvoiceEmail(String to, com.example.cake.payment.model.Invoice invoice) {
        if (to == null || to.isBlank() || invoice == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            String subject = "[CodeLearn] Hóa đơn thanh toán thành công - Mã HĐ: #" + invoice.getId();
            StringBuilder body = new StringBuilder();
            body.append("Cảm ơn bạn đã đăng ký khóa học tại CodeLearn!\n\n");
            body.append("--- THÔNG TIN HÓA ĐƠN ---\n");
            body.append("Mã hóa đơn: ").append(invoice.getId()).append("\n");
            body.append("Mã đơn hàng: ").append(invoice.getProviderOrderCode() != null ? invoice.getProviderOrderCode() : "N/A").append("\n");
            body.append("Khách hàng: ").append(invoice.getUserFullname()).append(" (").append(to).append(")\n");
            body.append("Số điện thoại: ").append(invoice.getUserPhoneNumber()).append("\n");
            body.append("Phương thức thanh toán: ").append(invoice.getPaymentMethod()).append("\n\n");

            body.append("--- DANH SÁCH KHÓA HỌC ---\n");
            if (invoice.getItems() != null) {
                for (var item : invoice.getItems()) {
                    long price = (item.getDiscountedPrice() != null && item.getDiscountedPrice() > 0)
                            ? item.getDiscountedPrice().longValue()
                            : (item.getPrice() != null ? item.getPrice().longValue() : 0L);
                    body.append("- ").append(item.getTitle()).append(": ").append(String.format("%,d", price)).append(" VNĐ\n");
                }
            }

            body.append("\nTỔNG TIỀN THANH TOÁN: ").append(String.format("%,d", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0L)).append(" VNĐ\n");
            body.append("Trạng thái: ĐÃ THANH TOÁN\n\n");
            body.append("Chúc bạn có trải nghiệm học tập tuyệt vời tại CodeLearn!\n");

            sendViaResendHttp(to, subject, body.toString());
        });
    }
}
