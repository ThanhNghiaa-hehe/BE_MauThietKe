package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.model.QuizAttempt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Template Method: shared scoring algorithm for option-based questions.
 */
public abstract class AbstractOptionBasedScoringStrategy implements QuestionScoringStrategy {

    /**
     * Template method: keep scoring flow stable across strategies.
     */
    protected final ScoredQuestion scoreTemplate(Quiz.Question question, List<String> selectedOptions) {
        List<String> correct = correctAnswers(question);
        List<String> selected = safeSelected(selectedOptions);

        boolean isCorrect = isCorrectSelection(question, selected, correct);
        int points = isCorrect ? safePoints(question) : 0;

        return new ScoredQuestion(
                points,
                buildSavedAnswer(question, selected, isCorrect, points),
                buildQuestionResult(question, selected, correct, isCorrect, points)
        );
    }

    /**
     * Hook method: each strategy defines correctness rule.
     */
    protected abstract boolean isCorrectSelection(Quiz.Question question, List<String> selected, List<String> correct);

    protected int safePoints(Quiz.Question q) {
        return q.getPoints() != null ? q.getPoints() : 0;
    }

    protected List<String> safeSelected(List<String> selected) {
        return selected != null ? selected : List.of();
    }

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
