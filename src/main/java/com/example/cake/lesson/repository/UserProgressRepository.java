package com.example.cake.lesson.repository;

import com.example.cake.lesson.model.UserProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends MongoRepository<UserProgress, String> {

    /**
     * Tìm tiến độ của user trong một khóa học
     */
    Optional<UserProgress> findByUserIdAndCourseId(String userId, String courseId);

    /**
     * Tìm tất cả tiến độ của một user
     */
    List<UserProgress> findByUserId(String userId);

    /**
     * Tìm tất cả user đã đăng ký một khóa học
     */
    List<UserProgress> findByCourseId(String courseId);

    /**
     * Đếm số user đã hoàn thành một khóa học
     */
    Long countByCourseIdAndCompletedAtIsNotNull(String courseId);

    /**
     * Xóa tiến độ của user trong một khóa học
     */
    void deleteByUserIdAndCourseId(String userId, String courseId);

    /**
     * Xóa tất cả tiến độ học tập của user
     */
    void deleteByUserId(String userId);
}

