package com.example.cake.quiz.service.event;

import com.example.cake.quiz.model.QuizAttempt;
import lombok.Builder;
import lombok.Value;

/**
 * Observer: fired after a quiz attempt is submitted and persisted.
 */
@Value
@Builder
public class QuizSubmittedEvent {
    String userId;
    String quizId;
    String lessonId;
    Double percentage;
    Boolean passed;
    QuizAttempt attempt;
}

