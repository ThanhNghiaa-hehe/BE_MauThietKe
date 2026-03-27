package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.model.QuizAttempt;

import java.util.List;

/**
 * Strategy: score a single question based on its type.
 */
public interface QuestionScoringStrategy {

    Quiz.QuestionType supports();

    /**
     * @param question quiz question definition
     * @param selectedOptions list of option ids selected by user (may be empty)
     */
    ScoredQuestion score(Quiz.Question question, List<String> selectedOptions);

    record ScoredQuestion(
            int pointsEarned,
            QuizAttempt.Answer savedAnswer,
            QuizResultResponse.QuestionResult questionResult
    ) {}
}

