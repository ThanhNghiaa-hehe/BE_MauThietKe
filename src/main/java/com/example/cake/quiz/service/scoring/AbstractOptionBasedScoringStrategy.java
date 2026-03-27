package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.model.QuizAttempt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared helper for option-based questions.
 */
public abstract class AbstractOptionBasedScoringStrategy implements QuestionScoringStrategy {

    protected List<String> correctAnswers(Quiz.Question question) {
        if (question.getOptions() == null) return new ArrayList<>();
        return question.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .map(Quiz.Option::getId)
                .collect(Collectors.toList());
    }

    protected QuizAttempt.Answer buildSavedAnswer(Quiz.Question question, List<String> selectedOptions, boolean isCorrect, int pointsEarned) {
        return QuizAttempt.Answer.builder()
                .questionId(question.getId())
                .selectedOptions(selectedOptions)
                .isCorrect(isCorrect)
                .pointsEarned(pointsEarned)
                .build();
    }

    protected QuizResultResponse.QuestionResult buildQuestionResult(Quiz.Question question,
                                                                    List<String> selectedOptions,
                                                                    List<String> correctAnswers,
                                                                    boolean isCorrect,
                                                                    int pointsEarned) {
        return QuizResultResponse.QuestionResult.builder()
                .questionId(question.getId())
                .question(question.getQuestion())
                .selectedOptions(selectedOptions)
                .correctAnswers(correctAnswers)
                .isCorrect(isCorrect)
                .pointsEarned(pointsEarned)
                .totalPoints(question.getPoints())
                .explanation(question.getExplanation())
                .build();
    }
}

