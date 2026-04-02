package com.example.cake.payment.service.validation;

import com.example.cake.course.model.Course;
import com.example.cake.course.repository.CourseRepository;
import com.example.cake.lesson.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validate:
 * - course exists
 * - course is published
 * - user is not already enrolled
 *
 * Keeps messages identical to previous PaymentFacade implementation.
 */
@Component
@RequiredArgsConstructor
public class CourseExistenceAndEligibilityHandler extends AbstractPaymentCreationHandler {

    private final CourseRepository courseRepository;
    private final UserProgressRepository userProgressRepository;

    @Override
    public void handle(PaymentCreationContext ctx) {
        if (ctx == null || ctx.getCourseIds() == null || ctx.getCourseIds().isEmpty()) {
            if (ctx != null) ctx.fail("Vui lòng chọn ít nhất một khóa học");
            return;
        }

        for (String courseId : ctx.getCourseIds()) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                ctx.fail("Không tìm thấy khóa học: " + courseId);
                return;
            }
            if (course.getIsPublished() == null || !course.getIsPublished()) {
                ctx.fail("Khóa học chưa được công khai: " + course.getTitle());
                return;
            }
            if (userProgressRepository.findByUserIdAndCourseId(ctx.getUserId(), courseId).isPresent()) {
                ctx.fail("Bạn đã đăng ký khóa học: " + course.getTitle());
                return;
            }
            ctx.getCourses().add(course);
        }

        next(ctx);
    }
}

