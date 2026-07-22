package com.example.cake.review.controller;

import com.example.cake.response.ResponseMessage;
import com.example.cake.review.model.Review;
import com.example.cake.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> getCourseReviews(@PathVariable String courseId) {
        return ResponseEntity.ok(reviewService.getCourseReviews(courseId));
    }

    @PostMapping
    public ResponseEntity<ResponseMessage<Review>> createOrUpdateReview(
            @RequestBody Map<String, Object> request,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        String courseId = (String) request.get("courseId");
        Object ratingObj = request.get("rating");
        Integer rating = ratingObj instanceof Number ? ((Number) ratingObj).intValue() : 5;
        String comment = (String) request.get("comment");

        return ResponseEntity.ok(reviewService.createOrUpdateReview(email, courseId, rating, comment));
    }
}
