package com.example.cake.lesson.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Curriculum view for authenticated user, includes unlock/progress info.
 */
@Data
@Builder
public class CurriculumUserViewDTO {

    private String courseId;
    private List<ChapterView> chapters;

    @Data
    @Builder
    public static class ChapterView {
        private String chapterId;
        private String title;
        private Integer order;
        private List<LessonView> lessons;
    }

    @Data
    @Builder
    public static class LessonView {
        private String lessonId;
        private String title;
        private Integer order;
        private Integer duration;
        private Boolean hasQuiz;

        // unlock + progress
        private Boolean unlocked;
        private Boolean completed;
        private Integer videoProgress;
        private Boolean quizPassed;
        private Integer quizScore;
    }
}

