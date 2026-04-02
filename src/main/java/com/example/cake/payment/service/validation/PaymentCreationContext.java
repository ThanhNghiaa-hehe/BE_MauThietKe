package com.example.cake.payment.service.validation;

import com.example.cake.course.model.Course;
import com.example.cake.payment.model.Payment;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Context passed through the validation chain when creating a payment.
 * This is internal-only and does NOT affect API contracts.
 */
@Data
@Builder
public class PaymentCreationContext {

    private String userId;
    private List<String> courseIds;

    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    @Builder.Default
    private List<Payment.PaymentCourseItem> paymentCourses = new ArrayList<>();

    private Long totalAmount;

    // Error handling
    private boolean valid;
    private String errorMessage;

    public void fail(String message) {
        this.valid = false;
        this.errorMessage = message;
    }

    public void ok() {
        this.valid = true;
        this.errorMessage = null;
    }
}

