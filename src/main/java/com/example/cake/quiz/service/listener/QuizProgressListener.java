package com.example.cake.quiz.service.listener;

import com.example.cake.lesson.service.ProgressService;
import com.example.cake.quiz.service.event.QuizSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer: updates UserProgress when a quiz is passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizProgressListener {

    private final ProgressService progressService;

    @EventListener
    public void onQuizSubmitted(QuizSubmittedEvent event) {
        if (!Boolean.TRUE.equals(event.getPassed())) {
            return;
        }
        if (event.getLessonId() == null || event.getLessonId().isBlank()) {
            return;
        }

        try {
            progressService.updateQuizPassed(event.getUserId(), event.getLessonId(), event.getPercentage());
            log.info("[QuizProgressListener] Updated quiz passed for user={}, lesson={}, percentage={}",
                    event.getUserId(), event.getLessonId(), event.getPercentage());
        } catch (Exception e) {
            log.error("[QuizProgressListener] Failed to update progress: {}", e.getMessage(), e);
        }
    }
}
