package com.example.cake.lesson.service;

import com.example.cake.course.model.Course;
import com.example.cake.course.repository.CourseRepository;
import com.example.cake.lesson.dto.ChapterProgressDTO;
import com.example.cake.lesson.dto.LessonCompleteResponse;
import com.example.cake.lesson.dto.MyCourseDTO;
import com.example.cake.lesson.dto.QuizResult;
import com.example.cake.lesson.dto.QuizSubmission;
import com.example.cake.lesson.model.Chapter;
import com.example.cake.lesson.model.Lesson;
import com.example.cake.lesson.model.UserProgress;
import com.example.cake.lesson.repository.ChapterRepository;
import com.example.cake.lesson.repository.LessonRepository;
import com.example.cake.lesson.repository.UserProgressRepository;
import com.example.cake.lesson.service.event.CourseCompletedEvent;
import com.example.cake.lesson.service.event.LessonCompletedEvent;
import com.example.cake.lesson.service.progress.ProgressContext;
import com.example.cake.lesson.service.progress.ProgressState;
import com.example.cake.lesson.service.progress.ProgressStateResolver;
import com.example.cake.lesson.service.progress.StateTransitionResult;
import com.example.cake.lesson.service.progress.UserProgressStatus;
import com.example.cake.quiz.repository.QuizAttemptRepository;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.cake.quiz.model.Quiz;
import com.example.cake.quiz.repository.QuizRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;

    // State + Observer wiring
    private final ProgressStateResolver stateResolver;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Khởi tạo tiến độ khi user đăng ký khóa học
     */
    public ResponseMessage<UserProgress> initializeProgress(String userId, String courseId) {
        // Kiểm tra đã có progress chưa
        UserProgress existing = progressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (existing != null) {
            return new ResponseMessage<>(true, "Progress already exists", existing);
        }

        UserProgress progress = UserProgress.builder()
                .userId(userId)
                .courseId(courseId)
                .completedLessons(new ArrayList<>())
                .currentLessonId(null)
                .totalProgress(0)
                .lessonsProgress(new ArrayList<>())
                .enrolledAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        // State hook
        stateResolver.resolve(progress).onEnroll(progress);

        progressRepository.save(progress);
        log.info("Initialized progress for user: {} in course: {}", userId, courseId);

        return new ResponseMessage<>(true, "Progress initialized", progress);
    }

    /**
     * Lấy tiến độ của user trong một khóa học
     */
    public ResponseMessage<UserProgress> getProgress(String userId, String courseId) {
        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);

        if (progress == null) {
            return new ResponseMessage<>(false, "Progress not found. User may not be enrolled in this course.", null);
        }

        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(progress);

        return new ResponseMessage<>(true, "Success", progress);
    }

    /**
     * Đánh dấu lesson đã hoàn thành
     */
    public ResponseMessage<UserProgress> markLessonComplete(String userId, String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return new ResponseMessage<>(false, "Lesson not found", null);
        }

        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, lesson.getCourseId()).orElse(null);
        if (progress == null) {
            return new ResponseMessage<>(false, "User not enrolled in this course", null);
        }

        // Rule: must be allowed to access lesson (sequential unlock, etc.)
        boolean canAccess = Boolean.TRUE.equals(canAccessLesson(userId, lessonId).getData());
        boolean alreadyCompleted = progress.isLessonCompleted(lessonId);

        ProgressState currentState = stateResolver.resolve(progress);
        ProgressContext ctx = new ProgressContext(progress, lesson, null, canAccess, alreadyCompleted);
        StateTransitionResult transition = currentState.completeLesson(ctx);
        if (!transition.isAllowed()) {
            return new ResponseMessage<>(false, transition.getMessage(), progress);
        }
        if (alreadyCompleted) {
            return new ResponseMessage<>(true, "Lesson already completed", progress);
        }

        // Apply domain changes
        progress.markLessonComplete(lessonId);

        UserProgress.LessonProgress lessonProgress = findOrCreateLessonProgress(progress, lessonId);
        lessonProgress.setCompleted(true);
        lessonProgress.setCompletedAt(LocalDateTime.now());
        lessonProgress.setVideoProgress(100);

        updateTotalProgress(progress, lesson.getCourseId());
        progress.setCurrentLessonId(lessonId);
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(progress);

        // Observer events
        eventPublisher.publishEvent(LessonCompletedEvent.builder()
                .userId(userId)
                .courseId(lesson.getCourseId())
                .lessonId(lessonId)
                .totalProgress(progress.getTotalProgress())
                .completedAt(lessonProgress.getCompletedAt())
                .build());

        if (progress.getCompletedAt() != null && stateResolver.resolve(progress).getStatus() == UserProgressStatus.COMPLETED) {
            eventPublisher.publishEvent(CourseCompletedEvent.builder()
                    .userId(userId)
                    .courseId(lesson.getCourseId())
                    .totalProgress(progress.getTotalProgress())
                    .completedAt(progress.getCompletedAt())
                    .build());
            stateResolver.resolve(progress).onCourseCompleted(progress);
        }

        log.info("User {} completed lesson {}", userId, lessonId);
        return new ResponseMessage<>(true, "Lesson marked as complete", progress);
    }

    /**
     * Lấy video progress đã lưu của một lesson
     * FE gọi API này khi load lesson để restore progress
     */
    public ResponseMessage<UserProgress.LessonProgress> getLessonProgress(String userId, String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return new ResponseMessage<>(false, "Lesson not found", null);
        }

        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, lesson.getCourseId()).orElse(null);
        if (progress == null) {
            // User chưa enroll khóa học
            return new ResponseMessage<>(false, "User not enrolled in this course", null);
        }

        // Tìm lesson progress
        UserProgress.LessonProgress lessonProgress = findLessonProgressById(progress, lessonId);

        if (lessonProgress == null) {
            // Lesson chưa có progress → Tạo mới với 0%
            lessonProgress = UserProgress.LessonProgress.builder()
                    .lessonId(lessonId)
                    .completed(false)
                    .videoProgress(0)
                    .timeSpent(0)
                    .quizAttempts(0)
                    .build();
        }

        log.debug("Retrieved lesson progress for user {} on lesson {}: {}%",
                userId, lessonId, lessonProgress.getVideoProgress());

        return new ResponseMessage<>(true, "Success", lessonProgress);
    }

    /**
     * Cập nhật tiến độ xem video
     */
    public ResponseMessage<UserProgress> updateVideoProgress(String userId, String lessonId, Integer percent) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return new ResponseMessage<>(false, "Lesson not found", null);
        }

        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, lesson.getCourseId()).orElse(null);
        if (progress == null) {
            return new ResponseMessage<>(false, "User not enrolled in this course", null);
        }

        boolean canAccess = Boolean.TRUE.equals(canAccessLesson(userId, lessonId).getData());
        boolean alreadyCompleted = progress.isLessonCompleted(lessonId);

        ProgressState currentState = stateResolver.resolve(progress);
        ProgressContext ctx = new ProgressContext(progress, lesson, percent, canAccess, alreadyCompleted);
        StateTransitionResult transition = currentState.updateVideoProgress(ctx);
        if (!transition.isAllowed()) {
            return new ResponseMessage<>(false, transition.getMessage(), progress);
        }

        UserProgress.LessonProgress lessonProgress = findOrCreateLessonProgress(progress, lessonId);
        lessonProgress.setVideoProgress(percent);

        if (percent != null && percent >= 90 && !Boolean.TRUE.equals(lessonProgress.getCompleted())) {
            // Use the same state rule for completion
            StateTransitionResult completeTransition = currentState.completeLesson(new ProgressContext(progress, lesson, percent, canAccess, alreadyCompleted));
            if (completeTransition.isAllowed()) {
                lessonProgress.setCompleted(true);
                lessonProgress.setCompletedAt(LocalDateTime.now());
                progress.markLessonComplete(lessonId);
                updateTotalProgress(progress, lesson.getCourseId());

                eventPublisher.publishEvent(LessonCompletedEvent.builder()
                        .userId(userId)
                        .courseId(lesson.getCourseId())
                        .lessonId(lessonId)
                        .totalProgress(progress.getTotalProgress())
                        .completedAt(lessonProgress.getCompletedAt())
                        .build());

                if (progress.getCompletedAt() != null && stateResolver.resolve(progress).getStatus() == UserProgressStatus.COMPLETED) {
                    eventPublisher.publishEvent(CourseCompletedEvent.builder()
                            .userId(userId)
                            .courseId(lesson.getCourseId())
                            .totalProgress(progress.getTotalProgress())
                            .completedAt(progress.getCompletedAt())
                            .build());
                    stateResolver.resolve(progress).onCourseCompleted(progress);
                }
            }
        }

        progress.setCurrentLessonId(lessonId);
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(progress);

        return new ResponseMessage<>(true, "Video progress updated", progress);
    }

    /**
     * Nộp bài quiz
     * @deprecated Use QuizService.submitQuiz() instead
     * This method will be removed in future versions
     */
    @Deprecated
    public ResponseMessage<QuizResult> submitQuiz(String userId, QuizSubmission submission) {
        // TODO: Migrate to use QuizService
        // For now, return error message directing users to new API
        return new ResponseMessage<>(false,
                "This API is deprecated. Please use POST /api/quizzes/submit with new format",
                null);
    }

    /**
     * OLD gradeQuiz method - DEPRECATED
     * @deprecated Quiz grading is now handled by QuizService
     */
    @Deprecated
    private QuizResult gradeQuizOld(QuizSubmission submission) {
        // This method is deprecated
        // Use QuizService.submitQuiz() instead
        return null;
    }

    /**
     * Kiểm tra xem user có quyền truy cập lesson không
     *
     * 2 trường hợp:
     * 1. User CHƯA MUA khóa học → Chỉ xem được lessons isFree (preview)
     * 2. User ĐÃ MUA khóa học → Unlock theo thứ tự (không quan tâm isFree)
     */
    public ResponseMessage<Boolean> canAccessLesson(String userId, String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return new ResponseMessage<>(false, "Lesson not found", null);
        }

        // Kiểm tra user đã enroll khóa học chưa
        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, lesson.getCourseId()).orElse(null);

        if (progress == null) {
            // User CHƯA MUA khóa học → Chỉ cho xem lessons miễn phí (preview)
            if (Boolean.TRUE.equals(lesson.getIsFree())) {
                return new ResponseMessage<>(true, "Access granted (free preview lesson)", true);
            } else {
                return new ResponseMessage<>(false, "User not enrolled in this course", false);
            }
        }

        // User ĐÃ MUA khóa học → Check unlock tuần tự
        // (Không quan tâm isFree, vì đã mua rồi thì tất cả lessons đều có quyền)

        // Nếu không có yêu cầu lesson trước → unlock (lesson đầu)
        if (lesson.getRequiredPreviousLesson() == null || lesson.getRequiredPreviousLesson().isEmpty()) {
            return new ResponseMessage<>(true, "Access granted", true);
        }

        String requiredLessonId = lesson.getRequiredPreviousLesson();

        // Kiểm tra lesson trước đã hoàn thành chưa
        if (!progress.isLessonCompleted(requiredLessonId)) {
            return new ResponseMessage<>(false, "Bạn cần hoàn thành bài học trước để mở khóa bài này", false);
        }

        // Kiểm tra videoProgress >= 90%
        UserProgress.LessonProgress lessonProgress = findLessonProgressById(progress, requiredLessonId);
        if (lessonProgress != null && lessonProgress.getVideoProgress() != null) {
            if (lessonProgress.getVideoProgress() < 90) {
                Lesson prevLesson = lessonRepository.findById(requiredLessonId).orElse(null);
                String prevLessonTitle = prevLesson != null ? prevLesson.getTitle() : "bài trước";
                return new ResponseMessage<>(false,
                        String.format("Bạn cần xem ít nhất 90%% video của '%s' để mở khóa bài này (Hiện tại: %d%%)",
                                prevLessonTitle, lessonProgress.getVideoProgress()),
                        false);
            }
        }

        return new ResponseMessage<>(true, "Access granted", true);
    }

    /**
     * Lấy thông tin lesson tiếp theo (API endpoint)
     */
    public ResponseMessage<LessonCompleteResponse> getNextLessonInfo(String userId, String currentLessonId) {
        Lesson currentLesson = lessonRepository.findById(currentLessonId).orElse(null);
        if (currentLesson == null) {
            return new ResponseMessage<>(false, "Lesson not found", null);
        }

        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, currentLesson.getCourseId()).orElse(null);
        if (progress == null) {
            return new ResponseMessage<>(false, "Progress not found", null);
        }

        LessonCompleteResponse response = createCompleteResponse(progress, currentLesson.getCourseId());
        return new ResponseMessage<>(true, "Next lesson info retrieved", response);
    }

    // ========== HELPER METHODS ==========

    private UserProgress.LessonProgress findOrCreateLessonProgress(UserProgress progress, String lessonId) {
        if (progress.getLessonsProgress() == null) {
            progress.setLessonsProgress(new ArrayList<>());
        }

        return progress.getLessonsProgress().stream()
                .filter(lp -> lp.getLessonId().equals(lessonId))
                .findFirst()
                .orElseGet(() -> {
                    UserProgress.LessonProgress newLp = UserProgress.LessonProgress.builder()
                            .lessonId(lessonId)
                            .completed(false)
                            .timeSpent(0)
                            .videoProgress(0)
                            .quizAttempts(0)
                            .build();
                    progress.getLessonsProgress().add(newLp);
                    return newLp;
                });
    }

    private void updateTotalProgress(UserProgress progress, String courseId) {
        Long totalLessons = lessonRepository.countByCourseId(courseId);
        if (totalLessons == 0) {
            progress.setTotalProgress(0);
            return;
        }

        int completedCount = progress.getCompletedLessons() != null ? progress.getCompletedLessons().size() : 0;
        int percent = (int) ((completedCount * 100.0) / totalLessons);
        progress.setTotalProgress(percent);

        // Nếu hoàn thành 100% → set completedAt
        if (percent >= 100 && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
            log.info("User {} completed course {}", progress.getUserId(), courseId);
        }
    }

    /*
     * OLD gradeQuiz - REMOVED
     * Quiz grading is now handled by QuizService
     * See: com.example.cake.quiz.service.QuizService.submitQuiz()
     */

    /**
     * Tìm lesson tiếp theo sau khi complete
     * CHỈ trả về lesson ĐÃ UNLOCK (có thể truy cập được)
     */
    private Lesson findNextLesson(Lesson currentLesson, UserProgress progress) {
        // 1. Tìm tất cả lessons sau current lesson trong cùng chapter
        List<Lesson> candidatesInChapter = lessonRepository.findByChapterIdAndOrderGreaterThanOrderByOrderAsc(
                currentLesson.getChapterId(),
                currentLesson.getOrder()
        );

        // 2. Tìm lesson đầu tiên đã unlock trong chapter
        for (Lesson lesson : candidatesInChapter) {
            if (isLessonUnlocked(lesson, progress)) {
                return lesson;
            }
        }

        // 3. Nếu không có lesson nào unlock trong chapter → Check quiz requirement
        Chapter currentChapter = chapterRepository.findById(currentLesson.getChapterId()).orElse(null);
        if (currentChapter == null) return null;

        // ✅ FIX: Kiểm tra lesson cuối chapter có quiz và đã pass chưa
        Lesson lastLessonInChapter = lessonRepository.findFirstByChapterIdOrderByOrderDesc(currentChapter.getId());
        if (lastLessonInChapter != null && Boolean.TRUE.equals(lastLessonInChapter.getHasQuiz())) {
            UserProgress.LessonProgress lastLessonProgress = findLessonProgressById(progress, lastLessonInChapter.getId());
            if (lastLessonProgress == null || lastLessonProgress.getQuizPassedAt() == null) {
                // Quiz chưa pass → Không unlock chapter tiếp theo
                log.debug("Cannot unlock next chapter: quiz of last lesson {} not passed yet",
                        lastLessonInChapter.getId());
                return null;
            }
        }

        // 4. Tìm chapter tiếp theo
        Chapter nextChapter = chapterRepository.findFirstByCourseIdAndOrderGreaterThanOrderByOrderAsc(
                currentChapter.getCourseId(),
                currentChapter.getOrder()
        );

        if (nextChapter == null) return null;

        // 5. Tìm lesson đầu tiên unlock trong chapter mới
        List<Lesson> candidatesInNextChapter = lessonRepository.findAllByChapterIdOrderByOrderAsc(nextChapter.getId());
        for (Lesson lesson : candidatesInNextChapter) {
            if (isLessonUnlocked(lesson, progress)) {
                return lesson;
            }
        }

        return null;  // Không có lesson nào unlock
    }

    /**
     * Kiểm tra lesson có unlock không (cho user ĐÃ ENROLL)
     *
     * Lưu ý: User đã mua khóa học rồi, nên KHÔNG check isFree
     * Check:
     * 1. Lesson trước đã completed
     * 2. VideoProgress của lesson trước >= 90%
     */
    private boolean isLessonUnlocked(Lesson lesson, UserProgress progress) {
        // Nếu không có yêu cầu lesson trước → unlock (lesson đầu tiên của chapter/course)
        if (lesson.getRequiredPreviousLesson() == null || lesson.getRequiredPreviousLesson().isEmpty()) {
            return true;
        }

        String requiredLessonId = lesson.getRequiredPreviousLesson();

        // 1. Kiểm tra lesson trước đã complete chưa
        if (!progress.isLessonCompleted(requiredLessonId)) {
            return false;
        }

        // 2. Kiểm tra videoProgress >= 90%
        UserProgress.LessonProgress lessonProgress = findLessonProgressById(progress, requiredLessonId);
        if (lessonProgress != null && lessonProgress.getVideoProgress() != null) {
            if (lessonProgress.getVideoProgress() < 90) {
                log.debug("Lesson {} locked: previous lesson {} video progress {}% < 90%",
                        lesson.getId(), requiredLessonId, lessonProgress.getVideoProgress());
                return false;
            }
        }

        return true;
    }

    /**
     * Helper method để tìm LessonProgress by lessonId
     */
    private UserProgress.LessonProgress findLessonProgressById(UserProgress progress, String lessonId) {
        if (progress.getLessonsProgress() == null) {
            return null;
        }
        return progress.getLessonsProgress().stream()
                .filter(lp -> lp.getLessonId().equals(lessonId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tạo response khi complete lesson với thông tin lesson tiếp theo
     */
    public LessonCompleteResponse createCompleteResponse(UserProgress progress, String courseId) {
        Long totalLessons = lessonRepository.countByCourseId(courseId);
        int completedCount = progress.getCompletedLessons() != null ? progress.getCompletedLessons().size() : 0;
        boolean courseCompleted = progress.getTotalProgress() != null && progress.getTotalProgress() >= 100;

        // Tìm lesson hiện tại
        Lesson currentLesson = lessonRepository.findById(progress.getCurrentLessonId()).orElse(null);
        LessonCompleteResponse.NextLesson nextLessonInfo = null;
        String message;
        String suggestedAction = null;
        String requiredLessonId = null;

        if (courseCompleted) {
            message = "🎉 Chúc mừng! Bạn đã hoàn thành khóa học!";
            suggestedAction = "COURSE_DONE";
        } else if (currentLesson != null) {
            // Tìm lesson tiếp theo ĐÃ UNLOCK
            Lesson nextLesson = findNextLesson(currentLesson, progress);

            if (nextLesson != null) {
                // Có lesson unlock tiếp theo
                Chapter nextChapter = chapterRepository.findById(nextLesson.getChapterId()).orElse(null);
                String chapterTitle = nextChapter != null ? nextChapter.getTitle() : "";

                nextLessonInfo = LessonCompleteResponse.NextLesson.fromLesson(
                    nextLesson,
                    chapterTitle,
                    true  // Luôn true vì đã filter unlock rồi
                );

                if (Boolean.TRUE.equals(currentLesson.getHasQuiz())) {
                    message = "✅ Quiz hoàn thành! Chuyển sang bài tiếp theo.";
                } else {
                    message = "✅ Lesson hoàn thành! Chuyển sang bài tiếp theo.";
                }
            } else {
                // Không có lesson unlock tiếp theo
                if (Boolean.TRUE.equals(currentLesson.getHasQuiz())) {
                    // Kiểm tra xem quiz có pass không
                    UserProgress.LessonProgress lessonProgress = findOrCreateLessonProgress(progress, currentLesson.getId());
                    boolean quizPassed = lessonProgress.getQuizPassedAt() != null;

                    if (!quizPassed) {
                        // Quiz chưa pass
                        message = "❌ Bạn cần hoàn thành quiz với điểm tối thiểu để mở khóa bài tiếp theo hoặc chương mới. Hãy làm lại quiz!";
                        suggestedAction = "RETAKE_QUIZ";
                        requiredLessonId = currentLesson.getId();
                    } else {
                        // Quiz đã pass nhưng vẫn chưa có lesson unlock
                        // Có thể là do lesson tiếp theo yêu cầu lesson khác hoặc hết khóa học
                        Lesson nextLockedLesson = findNextLessonIgnoreLock(currentLesson);
                        if (nextLockedLesson != null && nextLockedLesson.getRequiredPreviousLesson() != null) {
                            Lesson requiredLesson = lessonRepository.findById(nextLockedLesson.getRequiredPreviousLesson()).orElse(null);
                            if (requiredLesson != null) {
                                UserProgress.LessonProgress reqProgress = findLessonProgressById(progress, requiredLesson.getId());
                                if (reqProgress == null || reqProgress.getVideoProgress() == null || reqProgress.getVideoProgress() < 90) {
                                    message = String.format("⚠️ Bạn cần xem ít nhất 90%% video của '%s' để mở khóa bài tiếp theo.",
                                            requiredLesson.getTitle());
                                } else {
                                    message = String.format("⚠️ Bạn cần hoàn thành bài '%s' để mở khóa bài tiếp theo.",
                                            requiredLesson.getTitle());
                                }
                                suggestedAction = "COMPLETE_REQUIRED";
                                requiredLessonId = requiredLesson.getId();
                            } else {
                                message = "✅ Quiz hoàn thành! Hãy hoàn thành các bài yêu cầu khác để tiếp tục.";
                                suggestedAction = "COMPLETE_REQUIRED";
                            }
                        } else {
                            message = "✅ Quiz hoàn thành! Chương này đã hoàn thành.";
                        }
                    }
                } else {
                    // Lesson bình thường, có lesson tiếp nhưng bị lock
                    Lesson nextLockedLesson = findNextLessonIgnoreLock(currentLesson);
                    if (nextLockedLesson != null && nextLockedLesson.getRequiredPreviousLesson() != null) {
                        Lesson requiredLesson = lessonRepository.findById(nextLockedLesson.getRequiredPreviousLesson()).orElse(null);
                        if (requiredLesson != null) {
                            UserProgress.LessonProgress reqProgress = findLessonProgressById(progress, requiredLesson.getId());
                            if (reqProgress == null || reqProgress.getVideoProgress() == null || reqProgress.getVideoProgress() < 90) {
                                int currentProgress = (reqProgress != null && reqProgress.getVideoProgress() != null)
                                        ? reqProgress.getVideoProgress() : 0;
                                message = String.format("⚠️ Bạn cần xem ít nhất 90%% video của '%s' để mở khóa bài tiếp theo (Hiện tại: %d%%).",
                                        requiredLesson.getTitle(), currentProgress);
                            } else {
                                message = String.format("⚠️ Bạn cần hoàn thành bài '%s' để mở khóa bài tiếp theo.",
                                        requiredLesson.getTitle());
                            }
                            suggestedAction = "COMPLETE_REQUIRED";
                            requiredLessonId = requiredLesson.getId();
                        } else {
                            message = "⚠️ Hãy hoàn thành bài yêu cầu để mở khóa bài tiếp theo.";
                            suggestedAction = "COMPLETE_REQUIRED";
                        }
                    } else {
                        message = "✅ Bài học hoàn thành!";
                    }
                }
            }
        } else {
            message = "✅ Lesson hoàn thành!";
        }

        return LessonCompleteResponse.builder()
                .completed(true)
                .totalProgress(progress.getTotalProgress())
                .completedLessons(completedCount)
                .totalLessons(totalLessons.intValue())
                .nextLesson(nextLessonInfo)
                .message(message)
                .courseCompleted(courseCompleted)
                .suggestedAction(suggestedAction)
                .requiredLessonId(requiredLessonId)
                .build();
    }

    /**
     * Tìm lesson tiếp theo KHÔNG CHECK LOCK (để biết lesson nào đang block)
     */
    private Lesson findNextLessonIgnoreLock(Lesson currentLesson) {
        Lesson nextInChapter = lessonRepository.findFirstByChapterIdAndOrderGreaterThanOrderByOrderAsc(
                currentLesson.getChapterId(),
                currentLesson.getOrder()
        );

        if (nextInChapter != null) {
            return nextInChapter;
        }

        // Tìm chapter tiếp theo
        Chapter currentChapter = chapterRepository.findById(currentLesson.getChapterId()).orElse(null);
        if (currentChapter == null) return null;

        Chapter nextChapter = chapterRepository.findFirstByCourseIdAndOrderGreaterThanOrderByOrderAsc(
                currentChapter.getCourseId(),
                currentChapter.getOrder()
        );

        if (nextChapter == null) return null;

        return lessonRepository.findFirstByChapterIdOrderByOrderAsc(nextChapter.getId());
    }

    // ========== MY COURSES APIS ==========

    /**
     * Cập nhật quiz passed status (called by QuizService)
     */
    public ResponseMessage<UserProgress> updateQuizPassed(String userId, String lessonId, Double score) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return new ResponseMessage<>(false, "Lesson not found", null);
        }

        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, lesson.getCourseId()).orElse(null);
        if (progress == null) {
            return new ResponseMessage<>(false, "Progress not found", null);
        }

        UserProgress.LessonProgress lessonProgress = findOrCreateLessonProgress(progress, lessonId);
        lessonProgress.setQuizScore(score.intValue());
        lessonProgress.setQuizPassedAt(LocalDateTime.now());

        // Increment quiz attempts
        Integer attempts = lessonProgress.getQuizAttempts();
        lessonProgress.setQuizAttempts(attempts != null ? attempts + 1 : 1);

        progressRepository.save(progress);
        log.info("Updated quiz passed status for user {} on lesson {}, score: {}", userId, lessonId, score);

        return new ResponseMessage<>(true, "Quiz passed status updated", progress);
    }

    /**
     * Lấy danh sách khóa học user đã đăng ký (My Courses)
     */
    public ResponseMessage<java.util.List<MyCourseDTO>> getMyCourses(String userId) {
        // Lấy tất cả progress của user
        java.util.List<UserProgress> progressList = progressRepository.findByUserId(userId);

        if (progressList == null || progressList.isEmpty()) {
            return new ResponseMessage<>(true, "No enrolled courses", new java.util.ArrayList<>());
        }

        java.util.List<MyCourseDTO> myCourses = new java.util.ArrayList<>();

        for (UserProgress progress : progressList) {
            // Lấy thông tin course
            Course course = courseRepository.findById(progress.getCourseId()).orElse(null);
            if (course == null) continue;

            // Đếm tổng lessons
            Long totalLessons = lessonRepository.countByCourseId(progress.getCourseId());

            // Lấy tên lesson đang học
            String currentLessonTitle = null;
            if (progress.getCurrentLessonId() != null) {
                Lesson currentLesson = lessonRepository.findById(progress.getCurrentLessonId()).orElse(null);
                if (currentLesson != null) {
                    currentLessonTitle = currentLesson.getTitle();
                }
            }

            MyCourseDTO dto = MyCourseDTO.from(course, progress, totalLessons, currentLessonTitle);
            myCourses.add(dto);
        }

        // Sắp xếp theo lastAccessedAt (mới nhất trước)
        myCourses.sort((a, b) -> {
            if (a.getLastAccessedAt() == null) return 1;
            if (b.getLastAccessedAt() == null) return -1;
            return b.getLastAccessedAt().compareTo(a.getLastAccessedAt());
        });

        return new ResponseMessage<>(true, "My courses retrieved successfully", myCourses);
    }

    /**
     * Lấy danh sách chapters kèm progress của user
     */
    public ResponseMessage<java.util.List<ChapterProgressDTO>> getChaptersWithProgress(String userId, String courseId) {
        // Lấy progress của user
        UserProgress progress = progressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (progress == null) {
            return new ResponseMessage<>(false, "User not enrolled in this course", null);
        }

        // Lấy tất cả chapters
        java.util.List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
        if (chapters == null || chapters.isEmpty()) {
            return new ResponseMessage<>(false, "No chapters found", null);
        }

        java.util.List<ChapterProgressDTO> result = new java.util.ArrayList<>();

        for (Chapter chapter : chapters) {
            // Đếm lessons đã hoàn thành trong chapter
            java.util.List<Lesson> lessonsInChapter = lessonRepository.findAllByChapterIdOrderByOrderAsc(chapter.getId());
            int completedCount = 0;
            for (Lesson lesson : lessonsInChapter) {
                if (progress.isLessonCompleted(lesson.getId())) {
                    completedCount++;
                }
            }

            // Tìm bài Quiz gắn với ChapterId này từ QuizRepository
            String finalQuizId = null;
            Boolean quizPassed = null;
            Integer quizScore = null;

            List<Quiz> chapterQuizzes = quizRepository.findByChapterId(chapter.getId());
            if (chapterQuizzes != null && !chapterQuizzes.isEmpty()) {
                Quiz chapterQuiz = chapterQuizzes.get(0);
                finalQuizId = chapterQuiz.getId();

                boolean passed = quizAttemptRepository.existsByUserIdAndQuizIdAndPassedTrue(
                    progress.getUserId(),
                    chapterQuiz.getId()
                );
                quizPassed = passed;

                quizAttemptRepository.findFirstByUserIdAndQuizIdOrderByAttemptNumberDesc(progress.getUserId(), chapterQuiz.getId())
                    .ifPresent(attempt -> {
                        // score is integer %
                    });
            }

            // Check chapter unlock
            Boolean isUnlocked = isChapterUnlocked(chapter, progress, chapters);

            ChapterProgressDTO dto = ChapterProgressDTO.from(
                chapter,
                isUnlocked,
                completedCount,
                finalQuizId,
                quizPassed,
                quizScore
            );

            result.add(dto);
        }

        return new ResponseMessage<>(true, "Chapters with progress retrieved", result);
    }

    /**
     * Check xem chapter có unlock không
     */
    private Boolean isChapterUnlocked(Chapter chapter, UserProgress progress, java.util.List<Chapter> allChapters) {
        // Chapter đầu tiên luôn unlock
        if (chapter.getOrder() == 1) {
            return true;
        }

        // Tìm chapter trước
        Chapter previousChapter = allChapters.stream()
            .filter(c -> c.getOrder().equals(chapter.getOrder() - 1))
            .findFirst()
            .orElse(null);

        if (previousChapter == null) {
            return true; // Không có chapter trước → unlock
        }

        // 1. Phải hoàn thành TẤT CẢ các bài học của Chapter trước
        java.util.List<Lesson> lessonsInPreviousChapter = lessonRepository.findAllByChapterIdOrderByOrderAsc(previousChapter.getId());
        long completedInPrevious = lessonsInPreviousChapter.stream()
            .filter(l -> progress.isLessonCompleted(l.getId()))
            .count();

        if (completedInPrevious < lessonsInPreviousChapter.size()) {
            return false;
        }

        // 2. Nếu Chapter trước có bài Quiz → Phải PASS Bài Quiz đó
        List<Quiz> prevChapterQuizzes = quizRepository.findByChapterId(previousChapter.getId());
        if (prevChapterQuizzes != null && !prevChapterQuizzes.isEmpty()) {
            Quiz prevQuiz = prevChapterQuizzes.get(0);
            return quizAttemptRepository.existsByUserIdAndQuizIdAndPassedTrue(
                progress.getUserId(),
                prevQuiz.getId()
            );
        }

        // Nếu Chapter trước không có Quiz → Đã học hết bài → Mở khóa
        return true;
    }
}

