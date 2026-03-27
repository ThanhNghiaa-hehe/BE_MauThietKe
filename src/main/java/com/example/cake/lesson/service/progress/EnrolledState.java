package com.example.cake.lesson.service.progress;

import com.example.cake.lesson.model.UserProgress;

/**
 * ENROLLED: user just enrolled, hasn't completed any lesson yet.
 */
public class EnrolledState implements ProgressState {
    @Override
    public UserProgressStatus getStatus() {
        return UserProgressStatus.ENROLLED;
    }

    @Override
    public void onEnroll(UserProgress progress) {
        // no-op
    }

    @Override
    public StateTransitionResult completeLesson(ProgressContext ctx) {
        if (!ctx.isCanAccessLesson()) {
            return StateTransitionResult.deny("Bạn chưa có quyền truy cập bài học này (bị khoá theo thứ tự học)");
        }
        if (ctx.isAlreadyCompletedLesson()) {
            return StateTransitionResult.allow("Lesson already completed");
        }
        return StateTransitionResult.allow("OK");
    }

    @Override
    public StateTransitionResult updateVideoProgress(ProgressContext ctx) {
        if (!ctx.isCanAccessLesson()) {
            return StateTransitionResult.deny("Bạn chưa có quyền truy cập bài học này (bị khoá theo thứ tự học)");
        }
        return StateTransitionResult.allow("OK");
    }

    @Override
    public void onCourseCompleted(UserProgress progress) {
        // no-op
    }
}
