package com.example.cake.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(to);
        message.setSubject("Mã OTP xác minh");
        message.setText("Mã OTP của bạn là: " + otp + "\nHiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    public void sendOtpForgetPassWord(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(to);
        message.setSubject("Mã OTP Khôi phục mật khẩu ! ");
        message.setText("Mã OTP của bạn là : " + otp + "\n Hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    public void sendInvoiceEmail(String to, com.example.cake.payment.model.Invoice invoice) {
        if (to == null || to.isBlank() || invoice == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setTo(to);
            message.setSubject("[CodeLearn] Hóa đơn thanh toán thành công - Mã HĐ: #" + invoice.getId());

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
            body.append("Trạng thái: DÃ THANH TOÁN\n\n");
            body.append("Chúc bạn có trải nghiệm học tập tuyệt vời tại CodeLearn!\n");

            message.setText(body.toString());
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("⚠️ Send invoice email failed: " + ex.getMessage());
        }
    }
}
