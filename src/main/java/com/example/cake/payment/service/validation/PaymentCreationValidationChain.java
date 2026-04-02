package com.example.cake.payment.service.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds and runs the Chain of Responsibility for payment creation.
 *
 * This is internal-only and does NOT change endpoints or business rules.
 */
@Component
@RequiredArgsConstructor
public class PaymentCreationValidationChain {

    private final CourseExistenceAndEligibilityHandler courseHandler;
    private final BuildPaymentItemsAndTotalHandler itemsAndTotalHandler;

    public void validate(PaymentCreationContext ctx) {
        // Build chain order
        courseHandler.setNext(itemsAndTotalHandler);
        itemsAndTotalHandler.setNext(null);

        // Start chain
        courseHandler.handle(ctx);

        // If no handler failed, mark valid
        if (ctx.getErrorMessage() == null) {
            ctx.ok();
        }
    }
}

