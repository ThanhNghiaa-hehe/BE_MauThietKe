package com.example.cake.payment.service.gateway;

import com.example.cake.payment.model.Payment;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Method / Simple Factory for selecting a PaymentGateway by method.
 */
@Component
public class PaymentGatewayFactory {

    private final Map<Payment.PaymentMethod, PaymentGateway> gateways;

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList) {
        Map<Payment.PaymentMethod, PaymentGateway> map = new EnumMap<>(Payment.PaymentMethod.class);
        for (PaymentGateway gateway : gatewayList) {
            // Decorator: add logging/timing without changing gateway behavior
            PaymentGateway decorated = new LoggingPaymentGatewayDecorator(gateway);
            map.put(decorated.getMethod(), decorated);
        }
        this.gateways = Map.copyOf(map);
    }

    public PaymentGateway getGateway(Payment.PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return gateway;
    }
}
