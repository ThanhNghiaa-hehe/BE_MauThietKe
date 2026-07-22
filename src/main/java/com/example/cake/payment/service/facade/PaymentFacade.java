package com.example.cake.payment.service.facade;

import com.example.cake.course.model.Course;
import com.example.cake.course.repository.CourseRepository;
import com.example.cake.lesson.repository.UserProgressRepository;
import com.example.cake.payment.model.Payment;
import com.example.cake.payment.repository.PaymentRepository;
import com.example.cake.payment.service.event.PaymentSucceededEvent;
import com.example.cake.payment.service.gateway.GatewayCallbackResult;
import com.example.cake.payment.service.gateway.PaymentGateway;
import com.example.cake.payment.service.gateway.PaymentGatewayFactory;
import com.example.cake.payment.service.validation.PaymentCreationContext;
import com.example.cake.payment.service.validation.PaymentCreationValidationChain;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Facade: orchestrates payment lifecycle without leaking gateway-specific logic to controllers.
 */
import com.example.cake.coupon.service.CouponService;
import com.example.cake.payment.service.InvoiceService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final UserProgressRepository userProgressRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentCreationValidationChain paymentCreationValidationChain;
    private final CouponService couponService;
    private final InvoiceService invoiceService;

    public ResponseMessage<Map<String, String>> createPayment(
            String userId,
            List<String> courseIds,
            String orderInfo,
            String ipAddress,
            Payment.PaymentMethod method,
            String couponCode
    ) {
        try {
            PaymentCreationContext ctx = PaymentCreationContext.builder()
                    .userId(userId)
                    .courseIds(courseIds)
                    .build();

            paymentCreationValidationChain.validate(ctx);
            if (!ctx.isValid()) {
                return new ResponseMessage<>(false, ctx.getErrorMessage(), null);
            }

            List<Course> courses = ctx.getCourses();
            List<Payment.PaymentCourseItem> paymentCourses = ctx.getPaymentCourses();

            long totalPrice = ctx.getTotalAmount() != null ? ctx.getTotalAmount() : 0L;

            // Xử lý giảm giá từ Coupon nếu có
            if (couponCode != null && !couponCode.isBlank()) {
                try {
                    ResponseMessage<Map<String, Object>> couponRes = couponService.applyCoupon(couponCode, totalPrice);
                    if (couponRes.isSuccess() && couponRes.getData() != null) {
                        Object finalAmtObj = couponRes.getData().get("finalAmount");
                        if (finalAmtObj instanceof Number n) {
                            totalPrice = n.longValue();
                            log.info("🎟️ Applied coupon {} to payment: new amount={}", couponCode, totalPrice);
                        }
                    }
                } catch (Exception e) {
                    log.error("⚠️ Failed to apply coupon code {} in payment facade: {}", couponCode, e.getMessage());
                }
            }

            Payment payment = Payment.builder()
                    .userId(userId)
                    .courses(paymentCourses)
                    .amount(totalPrice)
                    .orderInfo(orderInfo)
                    .status(Payment.PaymentStatus.PENDING)
                    .paymentMethod(method)
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            PaymentGateway gateway = gatewayFactory.getGateway(method);
            String checkoutUrl = gateway.createPaymentUrl(payment, orderInfo, ipAddress);

            // Persist checkout url + provider orderCode for tracking/debug
            payment.setCheckoutUrl(checkoutUrl);
            paymentRepository.save(payment);

            // Tạo Hóa đơn PENDING ban đầu
            try {
                invoiceService.createInvoiceFromPayment(payment);
            } catch (Exception invEx) {
                log.error("⚠️ Failed to create initial pending invoice: {}", invEx.getMessage());
            }

            Map<String, String> result = new HashMap<>();
            result.put("checkoutUrl", checkoutUrl);
            // Keep stable alias across envs
            result.put("paymentUrl", checkoutUrl);
            result.put("paymentId", payment.getId());
            if (payment.getProviderOrderCode() != null) {
                result.put("orderCode", String.valueOf(payment.getProviderOrderCode()));
            }

            log.info("[PaymentFacade] Created payment paymentId={} method={} userId={} courses={} amount={} orderCode={} checkoutUrl={}",
                    payment.getId(), method, userId, courses.size(), payment.getAmount(), payment.getProviderOrderCode(), checkoutUrl);

            return new ResponseMessage<>(true, "Tạo link thanh toán thành công", result);

        } catch (Exception e) {
            log.error("[PaymentFacade] Error creating payment: {}", e.getMessage(), e);
            return new ResponseMessage<>(false, "Lỗi tạo thanh toán: " + e.getMessage(), null);
        }
    }

    /**
     * Process webhook/callback from gateway (PayOS).
     */
    public ResponseMessage<Map<String, Object>> processGatewayCallback(Payment.PaymentMethod method, Map<String, String> rawParams) {
        try {
            if (rawParams == null || rawParams.isEmpty()) {
                return new ResponseMessage<>(false, "Thiếu dữ liệu callback", null);
            }

            PaymentGateway gateway = gatewayFactory.getGateway(method);

            // Defensive copy for verification if gateway mutates map
            Map<String, String> paramsCopyForVerify = new HashMap<>(rawParams);
            if (!gateway.verifyCallbackSignature(paramsCopyForVerify)) {
                log.error("[PaymentFacade] Invalid signature for method={}", method);
                return new ResponseMessage<>(false, "Chữ ký không hợp lệ", null);
            }

            GatewayCallbackResult callback = gateway.parseCallback(rawParams);
            String callbackId = callback.getPaymentId();
            if (callbackId == null || callbackId.isBlank()) {
                return new ResponseMessage<>(false, "Thiếu mã giao dịch", null);
            }

            Payment payment;
            if (method == Payment.PaymentMethod.PAYOS) {
                // PayOS callback doesn't include our internal paymentId; it includes orderCode.
                Long orderCode;
                try {
                    orderCode = Long.valueOf(callbackId);
                } catch (Exception ex) {
                    return new ResponseMessage<>(false, "orderCode không hợp lệ", null);
                }
                payment = paymentRepository.findByProviderOrderCode(orderCode).orElse(null);
            } else {
                payment = paymentRepository.findById(callbackId).orElse(null);
            }

            if (payment == null) {
                log.error("[PaymentFacade] Payment not found for callbackId={} method={}", callbackId, method);
                return new ResponseMessage<>(false, "Không tìm thấy giao dịch", null);
            }

            if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "Giao dịch đã được xử lý");
                result.put("status", "success");
                return new ResponseMessage<>(true, "Giao dịch đã được xử lý", result);
            }

            payment.setProviderResponseCode(callback.getResponseCode());
            payment.setProviderTransactionId(callback.getTransactionNo());

            Map<String, Object> result = new HashMap<>();

            if (callback.isSuccess()) {
                payment.setStatus(Payment.PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                List<String> courseIds = payment.getCourses() == null
                        ? List.of()
                        : payment.getCourses().stream().map(Payment.PaymentCourseItem::getCourseId).toList();

                eventPublisher.publishEvent(PaymentSucceededEvent.builder()
                        .paymentId(payment.getId())
                        .userId(payment.getUserId())
                        .courseIds(courseIds)
                        .amount(payment.getAmount())
                        .paidAt(payment.getPaidAt())
                        .build());

                result.put("status", "success");
                result.put("message", "Thanh toán thành công");
                result.put("paymentId", payment.getId());
                result.put("amount", payment.getAmount());
                result.put("coursesEnrolled", courseIds.size());

                return new ResponseMessage<>(true, "Thanh toán thành công", result);
            }

            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            String message = gateway.getResponseMessage(callback.getResponseCode());
            result.put("status", "failed");
            result.put("message", message);
            result.put("responseCode", callback.getResponseCode());

            return new ResponseMessage<>(false, message, result);

        } catch (Exception e) {
            log.error("[PaymentFacade] Error processing callback: {}", e.getMessage(), e);
            return new ResponseMessage<>(false, "Lỗi xử lý thanh toán: " + e.getMessage(), null);
        }
    }
}
