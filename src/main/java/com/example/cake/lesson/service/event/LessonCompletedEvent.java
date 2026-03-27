package com.example.cake.lesson.service.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Observer pattern: fired when a user completes a lesson.
 */
@Value
@Builder
public class LessonCompletedEvent {
    String userId;
    String courseId;
    String lessonId;
    Integer totalProgress;
    LocalDateTime completedAt;
}

