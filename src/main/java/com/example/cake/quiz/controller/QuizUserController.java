package com.example.cake.quiz.controller;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.dto.QuizSubmitRequest;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.model.QuizAttempt;
import com.example.cake.quiz.service.QuizService;
import com.example.cake.response.ResponseMessage;
import com.example.cake.lesson.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User controller for quiz taking
 */
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizUserController {

    private final QuizService quizService;
    private final UserRepository userRepository;
    private final ProgressService progressService;

    /**
     * Helper method to get userId from Authentication
     */
    private String getUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    /**
     * Get quiz for student (without correct answers)
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseMessage<Quiz>> getQuiz(
            @PathVariable String quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.getQuizForStudent(quizId));
    }

    /**
     * Submit quiz
     */
    @PostMapping("/submit")
    public ResponseEntity<ResponseMessage<QuizResultResponse>> submitQuiz(
            @RequestBody QuizSubmitRequest request,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(quizService.submitQuiz(userId, request));
    }

    /**
     * Get quiz attempts history
     */
    @GetMapping("/{quizId}/attempts")
    public ResponseEntity<ResponseMessage<List<QuizAttempt>>> getAttempts(
            @PathVariable String quizId,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(quizService.getAttempts(userId, quizId));
    }

    /**
     * Check if user has passed quiz
     */
    @GetMapping("/{quizId}/passed")
    public ResponseEntity<ResponseMessage<Boolean>> hasPassedQuiz(
            @PathVariable String quizId,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(quizService.hasPassedQuiz(userId, quizId));
    }

    /**
     * Get quiz by lessonId for student (without correct answers)
     * User-safe endpoint to avoid calling admin APIs.
     */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ResponseMessage<Quiz>> getQuizByLessonId(
            @PathVariable String lessonId,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401)
                    .body(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        // Enforce the same access rule as lesson API
        String userId = getUserId(authentication);
        ResponseMessage<Boolean> access = progressService.canAccessLesson(userId, lessonId);
        if (!Boolean.TRUE.equals(access.getData())) {
            return ResponseEntity.status(403)
                    .body(new ResponseMessage<>(false, access.getMessage(), null));
        }

        ResponseMessage<Quiz> response = quizService.getQuizForStudentByLessonId(lessonId);
        if (!response.isSuccess() && "QUIZ_NOT_FOUND_FOR_LESSON".equals(response.getMessage())) {
            return ResponseEntity.ok(new ResponseMessage<>(true, "Lesson không có quiz", null));
        }
        return ResponseEntity.ok(response);
    }
}
