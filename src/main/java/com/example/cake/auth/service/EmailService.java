package com.example.cake.auth.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:seanpaul1402@gmail.com}")
    private String fromEmail;

    @PostConstruct
    public void initSanitizeMailConfig() {
        if (mailSender instanceof JavaMailSenderImpl senderImpl) {
            String rawPass = senderImpl.getPassword();
            if (rawPass != null && rawPass.contains(" ")) {
                String cleanPass = rawPass.replace(" ", "").trim();
                senderImpl.setPassword(cleanPass);
                log.info("🔒 [EmailService] Auto-sanitized Gmail App Password for Host environment");
            }

            // Force Port 465 SSL Direct for Cloud Host (Render blocks port 587)
            senderImpl.setHost("smtp.gmail.com");
            senderImpl.setPort(465);

            Properties props = senderImpl.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.timeout", "8000");
            props.put("mail.smtp.writetimeout", "8000");
        }
    }

    private String getSenderEmail() {
        return (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "seanpaul1402@gmail.com";
    }

    public void sendOtpEmail(String to, String otp) {
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
                helper.setFrom(getSenderEmail(), "CodeLearn");
                helper.setTo(to);
                helper.setSubject("Mã OTP xác minh - CodeLearn");
                helper.setText("Mã OTP của bạn là: " + otp + "\nHiệu lực trong 5 phút.");
                
                mailSender.send(mimeMessage);
                log.info("✅ [EmailService] OTP email sent successfully to {} from {}", to, getSenderEmail());
            } catch (Exception e) {
                log.error("❌ [EmailService] Failed to send OTP email to {}: {}", to, e.getMessage(), e);
            }
        });
    }

    public void sendOtpForgetPassWord(String to, String otp) {
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
                helper.setFrom(getSenderEmail(), "CodeLearn");
                helper.setTo(to);
                helper.setSubject("Mã OTP Khôi phục mật khẩu - CodeLearn");
                helper.setText("Mã OTP của bạn là: " + otp + "\nHiệu lực trong 5 phút.");
                
                mailSender.send(mimeMessage);
                log.info("✅ [EmailService] Forget password OTP sent successfully to {} from {}", to, getSenderEmail());
            } catch (Exception e) {
                log.error("❌ [EmailService] Failed to send forget password OTP to {}: {}", to, e.getMessage(), e);
            }
        });
    }

    public void sendInvoiceEmail(String to, com.example.cake.payment.model.Invoice invoice) {
        if (to == null || to.isBlank() || invoice == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
                helper.setFrom(getSenderEmail(), "CodeLearn");
                helper.setTo(to);
                helper.setSubject("[CodeLearn] Hóa đơn thanh toán thành công - Mã HĐ: #" + invoice.getId());

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

                helper.setText(body.toString(), false);
                mailSender.send(mimeMessage);
                log.info("✅ [EmailService] Invoice email sent successfully to {} from {}", to, getSenderEmail());
            } catch (Exception ex) {
                log.error("❌ [EmailService] Send invoice email failed to {}: {}", to, ex.getMessage(), ex);
            }
        });
    }
}
