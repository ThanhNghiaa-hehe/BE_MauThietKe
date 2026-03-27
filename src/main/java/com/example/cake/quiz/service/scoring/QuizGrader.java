package com.example.cake.quiz.service.scoring;

import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.dto.QuizSubmitRequest;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.model.QuizAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade-like helper for grading a quiz using Strategy + Factory.
 */
@Component
@RequiredArgsConstructor
public class QuizGrader {

    private final QuestionScoringStrategyFactory factory;

    public GradingResult grade(Quiz quiz, List<QuizSubmitRequest.AnswerRequest> userAnswers) {
        Map<String, List<String>> selectedMap = new HashMap<>();
        if (userAnswers != null) {
            for (QuizSubmitRequest.AnswerRequest a : userAnswers) {
                selectedMap.put(a.getQuestionId(), a.getSelectedOptions() != null ? a.getSelectedOptions() : List.of());
            }
        }

        int totalScore = 0;
        int earnedScore = 0;
        List<QuizAttempt.Answer> answers = new ArrayList<>();
        List<QuizResultResponse.QuestionResult> questionResults = new ArrayList<>();

        if (quiz.getQuestions() != null) {
            for (Quiz.Question question : quiz.getQuestions()) {
                int qPoints = question.getPoints() != null ? question.getPoints() : 0;
                totalScore += qPoints;

                List<String> selected = selectedMap.getOrDefault(question.getId(), List.of());
                QuestionScoringStrategy strategy = factory.get(question.getType());
                QuestionScoringStrategy.ScoredQuestion scored = strategy.score(question, selected);

                earnedScore += scored.pointsEarned();
                answers.add(scored.savedAnswer());
                questionResults.add(scored.questionResult());
            }
        }

        double percentage = totalScore > 0 ? ((double) earnedScore / totalScore) * 100 : 0;
        int passing = quiz.getPassingScore() != null ? quiz.getPassingScore() : 70;
        boolean passed = percentage >= passing;

        return new GradingResult(earnedScore, totalScore, percentage, passed, answers, questionResults);
    }

    public record GradingResult(
            int score,
            int totalScore,
            double percentage,
            boolean passed,
            List<QuizAttempt.Answer> answers,
            List<QuizResultResponse.QuestionResult> questionResults
    ) {}
}

