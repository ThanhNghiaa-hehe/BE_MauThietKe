package com.example.cake.payment.service.listener;

import com.example.cake.lesson.model.UserProgress;
import com.example.cake.lesson.repository.UserProgressRepository;
import com.example.cake.payment.model.Payment;
import com.example.cake.payment.repository.PaymentRepository;
import com.example.cake.payment.service.InvoiceService;
import com.example.cake.payment.service.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Observer (EventListener): enroll user to paid courses and generate invoice after payment success.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEnrollmentListener {

    private final UserProgressRepository userProgressRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        // 1. Tự động xuất hóa đơn (Invoice)
        try {
            Payment payment = paymentRepository.findById(event.getPaymentId()).orElse(null);
            if (payment != null) {
                invoiceService.createInvoiceFromPayment(payment);
            }
        } catch (Exception ex) {
            log.error("[PaymentEnrollmentListener] Failed to generate invoice for paymentId={}: {}",
                    event.getPaymentId(), ex.getMessage(), ex);
        }

        // 2. Ghi danh người dùng vào khóa học
        if (event.getCourseIds() == null || event.getCourseIds().isEmpty()) {
            return;
        }

        for (String courseId : event.getCourseIds()) {
            try {
                if (userProgressRepository.findByUserIdAndCourseId(event.getUserId(), courseId).isPresent()) {
                    continue;
                }

                UserProgress progress = UserProgress.builder()
                        .userId(event.getUserId())
                        .courseId(courseId)
                        .enrolledAt(event.getPaidAt() != null ? event.getPaidAt() : LocalDateTime.now())
                        .lastAccessedAt(LocalDateTime.now())
                        .completedLessons(new ArrayList<>())
                        .lessonsProgress(new ArrayList<>())
                        .totalProgress(0)
                        .build();

                userProgressRepository.save(progress);
                log.info("[PaymentEnrollmentListener] Enrolled user {} in course {} (paymentId={})",
                        event.getUserId(), courseId, event.getPaymentId());
            } catch (Exception e) {
                log.error("[PaymentEnrollmentListener] Failed to enroll user {} in course {} (paymentId={}): {}",
                        event.getUserId(), courseId, event.getPaymentId(), e.getMessage(), e);
            }
        }
    }
}
