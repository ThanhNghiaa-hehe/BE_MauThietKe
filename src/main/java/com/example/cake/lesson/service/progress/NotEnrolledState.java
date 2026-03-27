package com.example.cake.lesson.service.progress;

import com.example.cake.lesson.model.UserProgress;

/**
 * NOT_ENROLLED: user has no progress record.
 */
public class NotEnrolledState implements ProgressState {
    @Override
    public UserProgressStatus getStatus() {
        return UserProgressStatus.NOT_ENROLLED;
    }

    @Override
    public void onEnroll(UserProgress progress) {
        // no-op
    }

    @Override
    public StateTransitionResult completeLesson(ProgressContext ctx) {
        return StateTransitionResult.deny("User not enrolled in this course");
    }

    @Override
    public StateTransitionResult updateVideoProgress(ProgressContext ctx) {
        return StateTransitionResult.deny("User not enrolled in this course");
    }

    @Override
    public void onCourseCompleted(UserProgress progress) {
        // no-op
    }
}
