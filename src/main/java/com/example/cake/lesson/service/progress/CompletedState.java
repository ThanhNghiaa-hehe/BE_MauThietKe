package com.example.cake.lesson.service.progress;

import com.example.cake.lesson.model.UserProgress;

/**
 * COMPLETED: course finished.
 */
public class CompletedState implements ProgressState {
    @Override
    public UserProgressStatus getStatus() {
        return UserProgressStatus.COMPLETED;
    }

    @Override
    public void onEnroll(UserProgress progress) {
        // no-op
    }

    @Override
    public StateTransitionResult completeLesson(ProgressContext ctx) {
        return StateTransitionResult.deny("Course already completed");
    }

    @Override
    public StateTransitionResult updateVideoProgress(ProgressContext ctx) {
        return StateTransitionResult.deny("Course already completed");
    }

    @Override
    public void onCourseCompleted(UserProgress progress) {
        // no-op
    }
}
