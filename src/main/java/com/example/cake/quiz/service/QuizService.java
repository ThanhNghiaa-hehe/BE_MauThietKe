package com.example.cake.quiz.service;

import com.example.cake.quiz.dto.QuizRequest;
import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.dto.QuizSubmitRequest;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.model.QuizAttempt;
import com.example.cake.quiz.repository.QuizAttemptRepository;
import com.example.cake.quiz.repository.QuizRepository;
import com.example.cake.quiz.service.event.QuizSubmittedEvent;
import com.example.cake.quiz.service.scoring.QuizGrader;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final com.example.cake.lesson.service.ProgressService progressService;

    // Strategy + Factory
    private final QuizGrader quizGrader;

    // Observer
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Get all quizzes (admin)
     */
    public ResponseMessage<List<Quiz>> getAllQuizzes() {
        List<Quiz> quizzes = quizRepository.findAll();
        log.info("Retrieved {} quizzes", quizzes.size());
        return new ResponseMessage<>(true, "Success", quizzes);
    }

    /**
     * Create quiz
     */
    public ResponseMessage<Quiz> createQuiz(QuizRequest request) {
        // Convert request to Quiz entity
        List<Quiz.Question> questions = request.getQuestions().stream()
                .map(q -> Quiz.Question.builder()
                        .id(q.getId())
                        .question(q.getQuestion())
                        .type(Quiz.QuestionType.valueOf(q.getType()))
                        .points(q.getPoints())
                        .explanation(q.getExplanation())
                        .options(q.getOptions().stream()
                                .map(o -> Quiz.Option.builder()
                                        .id(o.getId())
                                        .text(o.getText())
                                        .isCorrect(o.getIsCorrect())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        Quiz quiz = Quiz.builder()
                .lessonId(request.getLessonId())
                .courseId(request.getCourseId())
                .chapterId(request.getChapterId())
                .title(request.getTitle())
                .description(request.getDescription())
                .passingScore(request.getPassingScore())
                .timeLimit(request.getTimeLimit())
                .maxAttempts(request.getMaxAttempts())
                .questions(questions)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        quiz = quizRepository.save(quiz);
        log.info("Created quiz: {}", quiz.getId());

        return new ResponseMessage<>(true, "Quiz created successfully", quiz);
    }

    /**
     * Get quiz by ID (for admin - show all answers)
     */
    public ResponseMessage<Quiz> getQuizById(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElse(null);

        if (quiz == null) {
            return new ResponseMessage<>(false, "Quiz not found", null);
        }

        return new ResponseMessage<>(true, "Success", quiz);
    }

    /**
     * Get quiz for student (hide correct answers)
     */
    public ResponseMessage<Quiz> getQuizForStudent(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElse(null);

        if (quiz == null) {
            return new ResponseMessage<>(false, "Quiz not found", null);
        }

        // Hide correct answers
        quiz.getQuestions().forEach(question -> {
            question.getOptions().forEach(option -> {
                option.setIsCorrect(null); // Hide correct flag
            });
        });

        return new ResponseMessage<>(true, "Success", quiz);
    }

    /**
     * Get quiz for student by lessonId (hide correct answers).
     */
    public ResponseMessage<Quiz> getQuizForStudentByLessonId(String lessonId) {
        Optional<Quiz> quizOpt = quizRepository.findByLessonId(lessonId);
        if (quizOpt.isEmpty()) {
            // Use a stable sentinel message so controller can convert to success=true,data=null if desired
            return new ResponseMessage<>(false, "QUIZ_NOT_FOUND_FOR_LESSON", null);
        }
        Quiz quiz = quizOpt.get();

        // Hide correct answers
        if (quiz.getQuestions() != null) {
            quiz.getQuestions().forEach(question -> {
                if (question.getOptions() != null) {
                    question.getOptions().forEach(option -> option.setIsCorrect(null));
                }
            });
        }

        return new ResponseMessage<>(true, "Success", quiz);
    }

    /**
     * Submit quiz
     */
    public ResponseMessage<QuizResultResponse> submitQuiz(String userId, QuizSubmitRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElse(null);

        if (quiz == null) {
            return new ResponseMessage<>(false, "Quiz not found", null);
        }

        // Check max attempts
        Integer attemptCount = attemptRepository.countByUserIdAndQuizId(userId, quiz.getId());
        if (quiz.getMaxAttempts() != null && attemptCount >= quiz.getMaxAttempts()) {
            return new ResponseMessage<>(false, "Bạn đã hết lượt làm quiz này", null);
        }

        // Grade quiz (Strategy + Factory)
        QuizGrader.GradingResult gradingResult = quizGrader.grade(quiz, request.getAnswers());

        // Calculate remaining attempts
        Integer remainingAttempts = null;
        if (quiz.getMaxAttempts() != null) {
            remainingAttempts = quiz.getMaxAttempts() - (attemptCount + 1);
        }

        // Save attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .userId(userId)
                .quizId(quiz.getId())
                .lessonId(quiz.getLessonId())
                .courseId(quiz.getCourseId())
                .attemptNumber(attemptCount + 1)
                .score(gradingResult.score())
                .totalScore(gradingResult.totalScore())
                .percentage(gradingResult.percentage())
                .passed(gradingResult.passed())
                .answers(gradingResult.answers())
                .timeSpent(request.getTimeSpent())
                .startedAt(request.getStartedAt())
                .completedAt(LocalDateTime.now())
                .build();

        attempt = attemptRepository.save(attempt);
        log.info("Saved quiz attempt: user={}, quiz={}, score={}/{}",
                userId, quiz.getId(), attempt.getScore(), attempt.getTotalScore());

        // Backward-compatible update (keep existing behavior)
        if (attempt.getPassed() && quiz.getLessonId() != null) {
            progressService.updateQuizPassed(userId, quiz.getLessonId(), attempt.getPercentage());
            log.info("✅ Updated quiz passed status in UserProgress for user={}, lesson={}",
                    userId, quiz.getLessonId());
        }

        // Observer: publish event for decoupled side effects (e.g., updating progress)
        eventPublisher.publishEvent(QuizSubmittedEvent.builder()
                .userId(userId)
                .quizId(quiz.getId())
                .lessonId(quiz.getLessonId())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .attempt(attempt)
                .build());

        // Create response
        QuizResultResponse response = QuizResultResponse.from(
                attempt,
                gradingResult.questionResults(),
                remainingAttempts
        );

        return new ResponseMessage<>(true, "Quiz submitted successfully", response);
    }

    /**
     * Get quiz attempts history
     */
    public ResponseMessage<List<QuizAttempt>> getAttempts(String userId, String quizId) {
        List<QuizAttempt> attempts = attemptRepository
                .findByUserIdAndQuizIdOrderByAttemptNumberDesc(userId, quizId);

        return new ResponseMessage<>(true, "Success", attempts);
    }

    /**
     * Check if user has passed quiz
     */
    public ResponseMessage<Boolean> hasPassedQuiz(String userId, String quizId) {
        boolean passed = attemptRepository.existsByUserIdAndQuizIdAndPassedTrue(userId, quizId);
        return new ResponseMessage<>(true, "Success", passed);
    }

    /**
     * Update quiz
     */
    public ResponseMessage<Quiz> updateQuiz(String quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElse(null);

        if (quiz == null) {
            return new ResponseMessage<>(false, "Quiz not found", null);
        }

        // Update fields
        if (request.getTitle() != null) quiz.setTitle(request.getTitle());
        if (request.getDescription() != null) quiz.setDescription(request.getDescription());
        if (request.getPassingScore() != null) quiz.setPassingScore(request.getPassingScore());
        if (request.getTimeLimit() != null) quiz.setTimeLimit(request.getTimeLimit());
        if (request.getMaxAttempts() != null) quiz.setMaxAttempts(request.getMaxAttempts());

        if (request.getQuestions() != null) {
            List<Quiz.Question> questions = request.getQuestions().stream()
                    .map(q -> Quiz.Question.builder()
                            .id(q.getId())
                            .question(q.getQuestion())
                            .type(Quiz.QuestionType.valueOf(q.getType()))
                            .points(q.getPoints())
                            .explanation(q.getExplanation())
                            .options(q.getOptions().stream()
                                    .map(o -> Quiz.Option.builder()
                                            .id(o.getId())
                                            .text(o.getText())
                                            .isCorrect(o.getIsCorrect())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
            quiz.setQuestions(questions);
        }

        quiz.setUpdatedAt(LocalDateTime.now());
        quiz = quizRepository.save(quiz);

        return new ResponseMessage<>(true, "Quiz updated successfully", quiz);
    }

    /**
     * Delete quiz
     */
    public ResponseMessage<Void> deleteQuiz(String quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) {
            return new ResponseMessage<>(false, "Quiz not found", null);
        }

        // Delete all attempts
        attemptRepository.deleteByQuizId(quizId);

        // Delete quiz
        quizRepository.deleteById(quizId);
        log.info("Deleted quiz: {}", quizId);

        return new ResponseMessage<>(true, "Quiz deleted successfully", null);
    }
}
