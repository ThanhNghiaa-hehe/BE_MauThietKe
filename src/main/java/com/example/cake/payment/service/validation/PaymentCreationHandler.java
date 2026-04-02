package com.example.cake.payment.service.validation;

/**
 * Chain of Responsibility: each handler validates/enriches the context,
 * then passes to the next handler.
 */
public interface PaymentCreationHandler {

    void setNext(PaymentCreationHandler next);

    void handle(PaymentCreationContext ctx);
}

