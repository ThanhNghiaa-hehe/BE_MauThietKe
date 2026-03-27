package com.example.cake.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment entity - Track payment transactions
 * Stores course info directly.
 */
@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;

    private String userId;

    private List<PaymentCourseItem> courses;

    /** Amount in VND */
    private Long amount;

    /** Human readable order info/description */
    private String orderInfo;

    // Gateway-neutral tracking
    private PaymentStatus status;
    private PaymentMethod paymentMethod;

    /** Provider-side order code (PayOS orderCode) */
    private Long providerOrderCode;

    /** Provider-side transaction/reference id (if any) */
    private String providerTransactionId;

    /** Provider response code/status */
    private String providerResponseCode;

    /** Checkout URL that user is redirected to */
    private String checkoutUrl;

    // Tracking
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    // Additional info
    private String ipAddress;

    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    public enum PaymentMethod {
        PAYOS
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentCourseItem {
        private String courseId;
        private String title;
        private String thumbnailUrl;
        private Double price;
        private Double discountedPrice;
        private Integer discountPercent;
        private String instructorName;
        private String level;
    }
}
