package com.example.cake.review.service;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.response.ResponseMessage;
import com.example.cake.review.model.Review;
import com.example.cake.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ResponseMessage<Map<String, Object>> getCourseReviews(String courseId) {
        List<Review> reviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);

        double avgRating = 5.0;
        if (!reviews.isEmpty()) {
            double sum = reviews.stream().mapToInt(r -> r.getRating() != null ? r.getRating() : 5).sum();
            avgRating = Math.round((sum / reviews.size()) * 10.0) / 10.0;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("reviews", reviews);
        data.put("avgRating", avgRating);
        data.put("totalReviews", reviews.size());

        return new ResponseMessage<>(true, "Lấy đánh giá thành công", data);
    }

    public ResponseMessage<Review> createOrUpdateReview(String email, String courseId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            return new ResponseMessage<>(false, "Số sao đánh giá phải từ 1 đến 5", null);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ResponseMessage<>(false, "Không tìm thấy người dùng", null);
        }

        Optional<Review> existingOpt = reviewRepository.findByCourseIdAndUserId(courseId, user.getId());
        Review review;
        if (existingOpt.isPresent()) {
            review = existingOpt.get();
            review.setRating(rating);
            review.setComment(comment != null ? comment.trim() : "");
            review.setCreatedAt(LocalDateTime.now());
        } else {
            review = Review.builder()
                    .courseId(courseId)
                    .userId(user.getId())
                    .userFullname(user.getFullname() != null ? user.getFullname() : user.getEmail())
                    .userEmail(user.getEmail())
                    .rating(rating)
                    .comment(comment != null ? comment.trim() : "")
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        Review saved = reviewRepository.save(review);
        log.info("[ReviewService] User {} rated course {} with {} stars", email, courseId, rating);
        return new ResponseMessage<>(true, "Gửi đánh giá khóa học thành công", saved);
    }
}
