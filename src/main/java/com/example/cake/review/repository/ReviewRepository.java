package com.example.cake.review.repository;

import com.example.cake.review.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByCourseIdOrderByCreatedAtDesc(String courseId);
    Optional<Review> findByCourseIdAndUserId(String courseId, String userId);
    List<Review> findAllByOrderByCreatedAtDesc();
    void deleteByUserId(String userId);
}
