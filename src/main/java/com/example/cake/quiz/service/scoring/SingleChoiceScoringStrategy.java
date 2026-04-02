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
        return scoreTemplate(question, selectedOptions);
    }

    @Override
    protected boolean isCorrectSelection(Quiz.Question question, List<String> selected, List<String> correct) {
        // Correct if user selected exactly 1 option and it matches the only correct option.
        return selected.size() == 1
                && correct.size() == 1
                && selected.get(0).equals(correct.get(0));
    }
}
