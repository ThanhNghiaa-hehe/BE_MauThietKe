package com.example.cake.payment.service.validation;

/**
 * Base handler implementation for payment creation validation chain.
 */
public abstract class AbstractPaymentCreationHandler implements PaymentCreationHandler {

    private PaymentCreationHandler next;

    @Override
    public void setNext(PaymentCreationHandler next) {
        this.next = next;
    }

    protected void next(PaymentCreationContext ctx) {
        if (next != null) {
            next.handle(ctx);
        }
    }
}
