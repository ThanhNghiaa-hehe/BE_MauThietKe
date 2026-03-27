package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.model.Quiz;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SINGLE_CHOICE: user must select exactly 1 correct option.
 */
@Component
public class SingleChoiceScoringStrategy extends AbstractOptionBasedScoringStrategy {

    @Override
    public Quiz.QuestionType supports() {
        return Quiz.QuestionType.SINGLE_CHOICE;
    }

    @Override
    public ScoredQuestion score(Quiz.Question question, List<String> selectedOptions) {
        List<String> correct = correctAnswers(question);

        // For single choice we consider correct if user selected exactly 1 option and it matches the only correct option.
        boolean isCorrect = selectedOptions != null
                && selectedOptions.size() == 1
                && correct.size() == 1
                && selectedOptions.get(0).equals(correct.get(0));

        int points = Boolean.TRUE.equals(isCorrect) ? safePoints(question) : 0;

        return new ScoredQuestion(
                points,
                buildSavedAnswer(question, safeSelected(selectedOptions), isCorrect, points),
                buildQuestionResult(question, safeSelected(selectedOptions), correct, isCorrect, points)
        );
    }

    private int safePoints(Quiz.Question q) {
        return q.getPoints() != null ? q.getPoints() : 0;
    }

    private List<String> safeSelected(List<String> selected) {
        return selected != null ? selected : List.of();
    }
}

