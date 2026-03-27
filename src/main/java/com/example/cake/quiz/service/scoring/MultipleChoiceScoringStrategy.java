package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.model.Quiz;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MULTIPLE_CHOICE: correct if user selected the exact set of correct answers.
 */
@Component
public class MultipleChoiceScoringStrategy extends AbstractOptionBasedScoringStrategy {

    @Override
    public Quiz.QuestionType supports() {
        return Quiz.QuestionType.MULTIPLE_CHOICE;
    }

    @Override
    public ScoredQuestion score(Quiz.Question question, List<String> selectedOptions) {
        List<String> correct = correctAnswers(question);
        List<String> selected = selectedOptions != null ? selectedOptions : List.of();

        boolean isCorrect = selected.size() == correct.size()
                && selected.containsAll(correct)
                && correct.containsAll(selected);

        int points = isCorrect ? safePoints(question) : 0;

        return new ScoredQuestion(
                points,
                buildSavedAnswer(question, selected, isCorrect, points),
                buildQuestionResult(question, selected, correct, isCorrect, points)
        );
    }

    private int safePoints(Quiz.Question q) {
        return q.getPoints() != null ? q.getPoints() : 0;
    }
}

