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
 * Invoice Entity - Stored in MongoDB "invoices" collection
 * Stores complete receipt & billing details for course purchase payments.
 */
@Document(collection = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    private String id;

    /** Internal Payment ID reference */
    private String paymentId;

    /** PayOS Order Code */
    private Long providerOrderCode;

    /** Gateway/Bank Transaction ID */
    private String providerTransactionId;

    // Customer Billing Information
    private String userId;
    private String userFullname;
    private String userEmail;
    private String userPhoneNumber;

    // Purchased Items
    private List<Payment.PaymentCourseItem> items;

    /** Total Amount Paid (VND) */
    private Long totalAmount;

    /** Payment Method (PAYOS) */
    private Payment.PaymentMethod paymentMethod;

    /** Invoice Status (PAID, CANCELLED, etc.) */
    private String status;

    /** Date & time invoice was issued */
    private LocalDateTime issuedAt;

    /** IP address used during purchase */
    private String ipAddress;

    /** Checkout URL for continuation of pending payment */
    private String checkoutUrl;
}
