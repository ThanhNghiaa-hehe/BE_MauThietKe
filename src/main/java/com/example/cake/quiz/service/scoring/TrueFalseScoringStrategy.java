package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.model.Quiz;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TRUE_FALSE: implemented as an option-based question with two options.
 * Correct if user's single selection matches the only correct option.
 */
@Component
public class TrueFalseScoringStrategy extends AbstractOptionBasedScoringStrategy {

    @Override
    public Quiz.QuestionType supports() {
        return Quiz.QuestionType.TRUE_FALSE;
    }

    @Override
    public ScoredQuestion score(Quiz.Question question, List<String> selectedOptions) {
        return scoreTemplate(question, selectedOptions);
    }

    @Override
    protected boolean isCorrectSelection(Quiz.Question question, List<String> selected, List<String> correct) {
        return selected.size() == 1
                && correct.size() == 1
                && selected.get(0).equals(correct.get(0));
    }
}
