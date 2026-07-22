package com.example.cake.payment.service;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.auth.service.EmailService;
import com.example.cake.course.model.Course;
import com.example.cake.course.repository.CourseRepository;
import com.example.cake.lesson.model.UserProgress;
import com.example.cake.lesson.repository.UserProgressRepository;
import com.example.cake.payment.model.Invoice;
import com.example.cake.payment.model.Payment;
import com.example.cake.payment.repository.InvoiceRepository;
import com.example.cake.payment.repository.PaymentRepository;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;
    private final CourseRepository courseRepository;
    private final EmailService emailService;

    /**
     * Tự động sinh Hóa đơn từ thông tin Payment thành công
     */
    public Invoice createInvoiceFromPayment(Payment payment) {
        if (payment == null) {
            log.error("[InvoiceService] Payment is null, cannot generate invoice");
            return null;
        }

        String targetStatus = "PENDING";
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            targetStatus = "PAID";
        } else if (payment.getStatus() == Payment.PaymentStatus.FAILED || payment.getStatus() == Payment.PaymentStatus.CANCELLED) {
            targetStatus = "CANCELLED";
        }

        // Kiểm tra xem hóa đơn cho payment này đã tồn tại chưa
        Optional<Invoice> existingInvoice = invoiceRepository.findByPaymentId(payment.getId());
        if (existingInvoice.isPresent()) {
            Invoice inv = existingInvoice.get();
            // Nếu giao dịch đã thành công mà hóa đơn chưa được cập nhật PAID
            if (payment.getStatus() == Payment.PaymentStatus.SUCCESS && !"PAID".equals(inv.getStatus())) {
                inv.setStatus("PAID");
                inv.setIssuedAt(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now());
                if (payment.getProviderTransactionId() != null) {
                    inv.setProviderTransactionId(payment.getProviderTransactionId());
                }
                Invoice updated = invoiceRepository.save(inv);
                try {
                    emailService.sendInvoiceEmail(updated.getUserEmail(), updated);
                    log.info("[InvoiceService] Sent receipt email for paymentId={}", payment.getId());
                } catch (Exception ex) {
                    log.error("[InvoiceService] Failed to send email invoice on payment success: {}", ex.getMessage());
                }
                return updated;
            }
            return inv;
        }

        // Lấy thông tin người mua
        User user = userRepository.findById(payment.getUserId()).orElse(null);

        Invoice invoice = Invoice.builder()
                .paymentId(payment.getId())
                .providerOrderCode(payment.getProviderOrderCode())
                .providerTransactionId(payment.getProviderTransactionId())
                .userId(payment.getUserId())
                .userFullname(user != null ? user.getFullname() : "N/A")
                .userEmail(user != null ? user.getEmail() : "N/A")
                .userPhoneNumber(user != null ? user.getPhoneNumber() : "N/A")
                .items(payment.getCourses())
                .totalAmount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(targetStatus)
                .checkoutUrl(payment.getCheckoutUrl())
                .issuedAt(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now())
                .ipAddress(payment.getIpAddress())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("[InvoiceService] Created invoice id={} status={} for userId={} amount={}",
                saved.getId(), saved.getStatus(), saved.getUserId(), saved.getTotalAmount());

        // CHỈ gửi email biên nhận khi hóa đơn mang trạng thái PAID
        if ("PAID".equals(saved.getStatus())) {
            try {
                emailService.sendInvoiceEmail(saved.getUserEmail(), saved);
                log.info("[InvoiceService] Sent receipt email for invoice id={}", saved.getId());
            } catch (Exception ex) {
                log.error("[InvoiceService] Failed to send email invoice: {}", ex.getMessage());
            }
        }

        return saved;
    }

    /**
     * Lấy danh sách tất cả hóa đơn của một người dùng
     * (Tự động đồng bộ từ tất cả Payment và các Khóa học UserProgress đang sở hữu)
     */
    public ResponseMessage<List<Invoice>> getUserInvoices(String userId) {
        try {
            // Đồng bộ trạng thái Hóa đơn từ danh sách Payment của User
            List<Payment> userPayments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (userPayments != null && !userPayments.isEmpty()) {
                for (Payment payment : userPayments) {
                    createInvoiceFromPayment(payment);
                }
            }
        } catch (Exception e) {
            log.error("[InvoiceService] Error syncing invoices for userId={}: {}", userId, e.getMessage(), e);
        }

        List<Invoice> invoices = invoiceRepository.findByUserIdOrderByIssuedAtDesc(userId);
        return new ResponseMessage<>(true, "Lấy danh sách hóa đơn thành công", invoices);
    }

    /**
     * Lấy chi tiết hóa đơn theo ID
     */
    public ResponseMessage<Invoice> getInvoiceById(String invoiceId) {
        Optional<Invoice> optional = invoiceRepository.findById(invoiceId);
        if (optional.isEmpty()) {
            return new ResponseMessage<>(false, "Không tìm thấy hóa đơn", null);
        }
        return new ResponseMessage<>(true, "Lấy chi tiết hóa đơn thành công", optional.get());
    }

    /**
     * Tra cứu hóa đơn theo Số điện thoại
     */
    public ResponseMessage<List<Invoice>> getInvoicesByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new ResponseMessage<>(false, "Vui lòng nhập số điện thoại", List.of());
        }
        String cleanPhone = phoneNumber.trim();
        List<Invoice> invoices = invoiceRepository.findByUserPhoneNumberOrderByIssuedAtDesc(cleanPhone);
        return new ResponseMessage<>(true, "Tra cứu hóa đơn theo SĐT thành công", invoices);
    }

    /**
     * Thống kê Doanh thu và tất cả Hóa đơn cho Admin
     */
    public ResponseMessage<java.util.Map<String, Object>> getAllInvoicesForAdmin() {
        List<Invoice> allInvoices = invoiceRepository.findAll();
        allInvoices.sort((a, b) -> {
            if (a.getIssuedAt() == null || b.getIssuedAt() == null) return 0;
            return b.getIssuedAt().compareTo(a.getIssuedAt());
        });

        double totalRevenue = 0.0;
        int paidCount = 0;
        int pendingCount = 0;

        for (Invoice inv : allInvoices) {
            if ("PAID".equalsIgnoreCase(inv.getStatus())) {
                if (inv.getTotalAmount() != null) {
                    totalRevenue += inv.getTotalAmount();
                }
                paidCount++;
            } else if ("PENDING".equalsIgnoreCase(inv.getStatus())) {
                pendingCount++;
            }
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("invoices", allInvoices);
        data.put("totalRevenue", totalRevenue);
        data.put("totalInvoices", allInvoices.size());
        data.put("paidCount", paidCount);
        data.put("pendingCount", pendingCount);

        return new ResponseMessage<>(true, "Lấy danh sách thống kê hóa đơn cho Admin thành công", data);
    }
}
