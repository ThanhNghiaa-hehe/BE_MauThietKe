package com.example.cake.payment.service.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event fired when a payment is confirmed as SUCCESS.
 */
@Value
@Builder
public class PaymentSucceededEvent {
    String paymentId;
    String userId;
    List<String> courseIds;
    Long amount;
    LocalDateTime paidAt;
}
