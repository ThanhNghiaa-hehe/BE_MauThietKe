package com.example.cake.payment.service.validation;

import com.example.cake.course.model.Course;
import com.example.cake.payment.model.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Build payment course items and total amount.
 * Keeps total calculation logic identical to previous PaymentFacade implementation.
 */
@Component
public class BuildPaymentItemsAndTotalHandler extends AbstractPaymentCreationHandler {

    @Override
    public void handle(PaymentCreationContext ctx) {
        List<Course> courses = ctx.getCourses();

        List<Payment.PaymentCourseItem> paymentCourses = courses.stream()
                .map(course -> Payment.PaymentCourseItem.builder()
                        .courseId(course.getId())
                        .title(course.getTitle())
                        .thumbnailUrl(course.getThumbnailUrl())
                        .price(course.getPrice())
                        .discountedPrice(course.getDiscountedPrice())
                        .discountPercent(course.getDiscountPercent())
                        .instructorName(course.getInstructorName())
                        .level(course.getLevel())
                        .build())
                .collect(Collectors.toList());

        ctx.setPaymentCourses(paymentCourses);

        double totalPriceDouble = paymentCourses.stream()
                .mapToDouble(item -> {
                    Double price = item.getDiscountedPrice() != null ? item.getDiscountedPrice() : item.getPrice();
                    return price != null ? price : 0.0;
                })
                .sum();

        long totalPrice = Math.round(totalPriceDouble);
        ctx.setTotalAmount(totalPrice);

        if (totalPrice <= 0) {
            ctx.fail("Tổng tiền không hợp lệ");
            return;
        }

        next(ctx);
    }
}

