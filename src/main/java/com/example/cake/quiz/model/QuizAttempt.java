package com.example.cake.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QuizAttempt - Track user's quiz submission history
 */
@Document(collection = "quiz_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {

    @Id
    private String id;

    private String userId;
    private String quizId;
    private String lessonId;
    private String courseId;

    private Integer attemptNumber;
    private Integer score;
    private Integer totalScore;
    private Double percentage;

    private Boolean passed;

    private List<Answer> answers;

    private Integer timeSpent;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // ========== NESTED CLASS ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Answer {
        private String questionId;
        private List<String> selectedOptions;  // Selected option IDs
        private Boolean isCorrect;
        private Integer pointsEarned;
    }
}

