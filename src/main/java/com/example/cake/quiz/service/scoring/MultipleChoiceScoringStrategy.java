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
        return scoreTemplate(question, selectedOptions);
    }

    @Override
    protected boolean isCorrectSelection(Quiz.Question question, List<String> selected, List<String> correct) {
        // Must match exact set (order independent)
        return selected.size() == correct.size()
                && selected.containsAll(correct)
                && correct.containsAll(selected);
    }
}
