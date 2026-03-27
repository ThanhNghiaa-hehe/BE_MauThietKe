package com.example.cake.lesson.service.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Observer pattern: fired when a user completes a course.
 */
@Value
@Builder
public class CourseCompletedEvent {
    String userId;
    String courseId;
    Integer totalProgress;
    LocalDateTime completedAt;
}

