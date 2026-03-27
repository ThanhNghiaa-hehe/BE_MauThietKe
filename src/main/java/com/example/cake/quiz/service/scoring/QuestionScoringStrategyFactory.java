package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.model.Quiz;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Method: resolve a QuestionScoringStrategy for a given QuestionType.
 */
@Component
public class QuestionScoringStrategyFactory {

    private final Map<Quiz.QuestionType, QuestionScoringStrategy> map;

    public QuestionScoringStrategyFactory(List<QuestionScoringStrategy> strategies) {
        Map<Quiz.QuestionType, QuestionScoringStrategy> tmp = new EnumMap<>(Quiz.QuestionType.class);
        for (QuestionScoringStrategy s : strategies) {
            tmp.put(s.supports(), s);
        }
        this.map = Map.copyOf(tmp);
    }

    public QuestionScoringStrategy get(Quiz.QuestionType type) {
        QuestionScoringStrategy s = map.get(type);
        if (s == null) {
            throw new IllegalArgumentException("Unsupported question type: " + type);
        }
        return s;
    }
}

