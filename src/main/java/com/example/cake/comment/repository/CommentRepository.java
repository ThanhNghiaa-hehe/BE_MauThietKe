package com.example.cake.comment.repository;

import com.example.cake.comment.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByLessonIdOrderByCreatedAtDesc(String lessonId);
    List<Comment> findByCourseIdOrderByCreatedAtDesc(String courseId);
    List<Comment> findAllByOrderByCreatedAtDesc();
}
