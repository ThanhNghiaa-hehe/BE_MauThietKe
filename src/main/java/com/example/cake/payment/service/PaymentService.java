package com.example.cake.payment.service;

import com.example.cake.payment.model.Payment;
import com.example.cake.payment.repository.PaymentRepository;
import com.example.cake.payment.service.facade.PaymentFacade;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * PaymentService (Application service)
 *
 * Kept to preserve existing controller calls.
 * - Create/callback orchestration is handled by {@link PaymentFacade} (Facade + Strategy/Factory + Observer).
 * - Simple read queries are handled directly via {@link PaymentRepository}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentFacade paymentFacade;
    private final PaymentRepository paymentRepository;

    /**
     * Create PayOS payment.
     */
    public ResponseMessage<Map<String, String>> createPayOSPayment(
            String userId,
            List<String> courseIds,
            String orderInfo,
            String ipAddress,
            String couponCode
    ) {
        return paymentFacade.createPayment(userId, courseIds, orderInfo, ipAddress, Payment.PaymentMethod.PAYOS, couponCode);
    }

    /**
     * Process PayOS return/ipn callback.
     */
    public ResponseMessage<Map<String, Object>> processPayOSCallback(Map<String, String> params) {
        return paymentFacade.processGatewayCallback(Payment.PaymentMethod.PAYOS, params);
    }

    /**
     * Get payment status.
     */
    public ResponseMessage<Payment> getPaymentStatus(String paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                return new ResponseMessage<>(false, "Không tìm thấy giao dịch", null);
            }
            return new ResponseMessage<>(true, "Success", payment);
        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage(), e);
            return new ResponseMessage<>(false, "Lỗi lấy trạng thái thanh toán", null);
        }
    }

    /**
     * Get all payments for a user.
     */
    public ResponseMessage<List<Payment>> getUserPayments(String userId) {
        try {
            List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return new ResponseMessage<>(true, "Lấy lịch sử thanh toán thành công", payments);
        } catch (Exception e) {
            log.error("Error getting user payments: {}", e.getMessage(), e);
            return new ResponseMessage<>(false, "Lỗi lấy lịch sử thanh toán", null);
        }
    }

    /**
     * Get successful payments for a user.
     */
    public ResponseMessage<List<Payment>> getUserSuccessfulPayments(String userId) {
        try {
            List<Payment> payments = paymentRepository.findByUserIdAndStatus(userId, Payment.PaymentStatus.SUCCESS);
            return new ResponseMessage<>(true, "Lấy lịch sử thanh toán thành công", payments);
        } catch (Exception e) {
            log.error("Error getting user successful payments: {}", e.getMessage(), e);
            return new ResponseMessage<>(false, "Lỗi lấy lịch sử thanh toán", null);
        }
    }
}
