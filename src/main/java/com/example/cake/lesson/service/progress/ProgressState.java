package com.example.cake.lesson.service.progress;

import com.example.cake.lesson.model.UserProgress;

/**
 * State pattern: represents user's course progress lifecycle state.
 */
public interface ProgressState {

    UserProgressStatus getStatus();

    /**
     * Called when user enrolls (progress is initialized).
     */
    void onEnroll(UserProgress progress);

    /**
     * Validate and handle lesson completion action.
     */
    StateTransitionResult completeLesson(ProgressContext ctx);

    /**
     * Validate and handle video progress update action.
     */
    StateTransitionResult updateVideoProgress(ProgressContext ctx);

    /**
     * Called when course is completed (100%).
     */
    void onCourseCompleted(UserProgress progress);
}
