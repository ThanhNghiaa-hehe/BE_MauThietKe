package com.example.cake.lesson.controller;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.lesson.dto.LessonCompleteResponse;
import com.example.cake.lesson.dto.QuizSubmission;
import com.example.cake.lesson.model.Lesson;
import com.example.cake.lesson.model.UserProgress;
import com.example.cake.lesson.service.LessonService;
import com.example.cake.lesson.service.ProgressService;
import com.example.cake.quiz.dto.QuizSubmitRequest;
import com.example.cake.quiz.dto.QuizResultResponse;
import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.repository.QuizRepository;
import com.example.cake.quiz.service.QuizService;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * User controller for accessing lessons and tracking progress
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonUserController {

    private final LessonService lessonService;
    private final ProgressService progressService;
    private final UserRepository userRepository;
    private final QuizService quizService;
    private final QuizRepository quizRepository;

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
     * Lấy thông tin lesson (có kiểm tra quyền truy cập)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage<Lesson>> getLesson(
            @PathVariable String id,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);

        // Kiểm tra quyền truy cập
        ResponseMessage<Boolean> access = progressService.canAccessLesson(userId, id);
        if (!Boolean.TRUE.equals(access.getData())) {
            return ResponseEntity.status(403).body(new ResponseMessage<>(false, access.getMessage(), null));
        }

        return ResponseEntity.ok(lessonService.getLessonById(id));
    }

    /**
     * Like lesson
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ResponseMessage<Lesson>> likeLesson(@PathVariable String id) {
        return ResponseEntity.ok(lessonService.likeLesson(id));
    }

    /**
     * Đánh dấu lesson hoàn thành
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ResponseMessage<UserProgress>> markLessonComplete(
            @PathVariable String id,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(progressService.markLessonComplete(userId, id));
    }

    /**
     * Lấy video progress đã lưu (để FE restore khi reload page)
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<ResponseMessage<UserProgress.LessonProgress>> getVideoProgress(
            @PathVariable String id,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(progressService.getLessonProgress(userId, id));
    }

    /**
     * Cập nhật tiến độ video
     */
    @PostMapping("/{id}/progress")
    public ResponseEntity<ResponseMessage<UserProgress>> updateVideoProgress(
            @PathVariable String id,
            @RequestParam Integer percent,
            Authentication authentication
    ) {
        if (percent == null || percent < 0 || percent > 100) {
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage<>(false, "percent phải nằm trong khoảng 0..100", null));
        }
        String userId = getUserId(authentication);
        return ResponseEntity.ok(progressService.updateVideoProgress(userId, id, percent));
    }

    /**
     * Nộp bài quiz (LEGACY)
     * @deprecated Use POST /api/quizzes/submit instead.
     * Backward-compatible proxy mapping legacy payload to new QuizSubmitRequest.
     */
    @Deprecated
    @PostMapping("/quiz/submit")
    public ResponseEntity<ResponseMessage<QuizResultResponse>> submitQuiz(
            @RequestBody QuizSubmission submission,
            Authentication authentication
    ) {
        try {
            String userId = getUserId(authentication);
            if (submission == null || submission.getLessonId() == null || submission.getLessonId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage<>(false, "lessonId không được để trống", null));
            }

            Quiz quiz = quizRepository.findByLessonId(submission.getLessonId()).orElse(null);
            if (quiz == null) {
                return ResponseEntity.ok(new ResponseMessage<>(false, "Quiz not found for lesson", null));
            }

            QuizSubmitRequest mapped = QuizSubmitRequest.builder()
                    .quizId(quiz.getId())
                    .answers(submission.getAnswers() == null ? java.util.List.of() :
                            submission.getAnswers().stream()
                                    .map(a -> QuizSubmitRequest.AnswerRequest.builder()
                                            .questionId(a.getQuestionId())
                                            .selectedOptions(a.getSelectedOptions())
                                            .build())
                                    .collect(Collectors.toList())
                    )
                    .timeSpent(null)
                    .startedAt(LocalDateTime.now())
                    .build();

            // Proxy to new quiz service
            ResponseMessage<QuizResultResponse> result = quizService.submitQuiz(userId, mapped);

            // Add a deprecation hint FE can display
            if (result != null && result.isSuccess()) {
                result.setMessage(result.getMessage() + " (via deprecated /api/lessons/quiz/submit - please migrate to /api/quizzes/submit)");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ResponseMessage<>(false, "Lỗi submit quiz legacy: " + e.getMessage(), null));
        }
    }

    /**
     * Kiểm tra quyền truy cập lesson
     */
    @GetMapping("/{id}/access")
    public ResponseEntity<ResponseMessage<Boolean>> checkAccess(
            @PathVariable String id,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(progressService.canAccessLesson(userId, id));
    }

    /**
     * Lấy thông tin lesson tiếp theo sau khi complete
     */
    @GetMapping("/{id}/next")
    public ResponseEntity<ResponseMessage<LessonCompleteResponse>> getNextLessonInfo(
            @PathVariable String id,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        ResponseMessage<LessonCompleteResponse> response = progressService.getNextLessonInfo(userId, id);
        return ResponseEntity.ok(response);
    }
}
