package com.example.cake.lesson.service.progress;

import com.example.cake.lesson.model.Lesson;
import com.example.cake.lesson.model.UserProgress;
import lombok.Getter;

/**
 * State pattern context: holds runtime data for state transitions.
 */
@Getter
public class ProgressContext {

    private final UserProgress progress;
    private final Lesson lesson;
    private final Integer videoPercent;

    private final boolean canAccessLesson;
    private final boolean alreadyCompletedLesson;

    public ProgressContext(UserProgress progress,
                           Lesson lesson,
                           Integer videoPercent,
                           boolean canAccessLesson,
                           boolean alreadyCompletedLesson) {
        this.progress = progress;
        this.lesson = lesson;
        this.videoPercent = videoPercent;
        this.canAccessLesson = canAccessLesson;
        this.alreadyCompletedLesson = alreadyCompletedLesson;
    }
}

