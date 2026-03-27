package com.example.cake.lesson.service.progress;

import com.example.cake.lesson.model.UserProgress;
import org.springframework.stereotype.Component;

/**
 * Resolves current state from persisted UserProgress data.
 */
@Component
public class ProgressStateResolver {

    public ProgressState resolve(UserProgress progress) {
        if (progress == null) {
            return new NotEnrolledState();
        }
        if (progress.getCompletedAt() != null) {
            return new CompletedState();
        }
        if (progress.getCompletedLessons() != null && !progress.getCompletedLessons().isEmpty()) {
            return new InProgressState();
        }
        return new EnrolledState();
    }
}
