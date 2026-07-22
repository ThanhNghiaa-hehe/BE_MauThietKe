package com.example.cake.payment.service.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * Normalized callback data returned from a payment gateway.
 */
@Getter
@Builder
public class GatewayCallbackResult {
    private final String paymentId;
    private final String responseCode;
    private final String transactionNo;
    private final String bankCode;

    public boolean isSuccess() {
        return "00".equalsIgnoreCase(responseCode) || "SUCCESS".equalsIgnoreCase(responseCode);
    }
}
