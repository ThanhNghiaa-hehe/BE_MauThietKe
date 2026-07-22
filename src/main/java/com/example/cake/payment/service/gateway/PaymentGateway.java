package com.example.cake.payment.service.gateway;

import com.example.cake.payment.model.Payment;

import java.util.Map;

/**
 * Strategy interface for payment gateways (PayOS, MoMo, ZaloPay, ...).
 */
public interface PaymentGateway {

    Payment.PaymentMethod getMethod();

    /**
     * Create a hosted payment URL (redirect URL) for the given payment.
     */
    String createPaymentUrl(Payment payment, String orderInfo, String ipAddress);

    /**
     * Verify the callback signature from gateway.
     */
    boolean verifyCallbackSignature(Map<String, String> params);

    /**
     * Convert raw callback params to a normalized result.
     */
    GatewayCallbackResult parseCallback(Map<String, String> params);

    /**
     * Get gateway-defined message for response code.
     */
    String getResponseMessage(String responseCode);
}
