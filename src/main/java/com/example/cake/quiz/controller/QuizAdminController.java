package com.example.cake.quiz.controller;

import com.example.cake.quiz.dto.QuizRequest;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.service.QuizService;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for quiz management
 */
@RestController
@RequestMapping("/api/admin/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class QuizAdminController {

    private final QuizService quizService;

    /**
     * Get all quizzes (admin)
     */
    @GetMapping("/all")
    public ResponseEntity<ResponseMessage<List<Quiz>>> getAllQuizzes(
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }

    /**
     * Create quiz
     */
    @PostMapping("/create")
    public ResponseEntity<ResponseMessage<Quiz>> createQuiz(
            @RequestBody QuizRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.createQuiz(request));
    }

    /**
     * Get quiz by ID (with correct answers)
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseMessage<Quiz>> getQuiz(
            @PathVariable String quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.getQuizById(quizId));
    }

    /**
     * Update quiz
     */
    @PutMapping("/{quizId}")
    public ResponseEntity<ResponseMessage<Quiz>> updateQuiz(
            @PathVariable String quizId,
            @RequestBody QuizRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.updateQuiz(quizId, request));
    }

    /**
     * Delete quiz
     */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<ResponseMessage<Void>> deleteQuiz(
            @PathVariable String quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.deleteQuiz(quizId));
    }
}
