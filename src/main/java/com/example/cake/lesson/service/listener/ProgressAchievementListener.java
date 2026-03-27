package com.example.cake.lesson.service.listener;

import com.example.cake.lesson.service.event.CourseCompletedEvent;
import com.example.cake.lesson.service.event.LessonCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer: reacts to progress events.
 * For now, this only logs. You can extend to send emails, badges, analytics, etc.
 */
@Slf4j
@Component
public class ProgressAchievementListener {

    @EventListener
    public void onLessonCompleted(LessonCompletedEvent event) {
        log.info("[ProgressAchievementListener] User {} completed lesson {} in course {} (progress={}%)",
                event.getUserId(), event.getLessonId(), event.getCourseId(), event.getTotalProgress());
    }

    @EventListener
    public void onCourseCompleted(CourseCompletedEvent event) {
        log.info("[ProgressAchievementListener] User {} COMPLETED course {} at {}",
                event.getUserId(), event.getCourseId(), event.getCompletedAt());
    }
}
