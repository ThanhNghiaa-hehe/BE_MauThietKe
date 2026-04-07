package com.example.cake.lesson.service;

import com.example.cake.lesson.dto.CurriculumUserViewDTO;
import com.example.cake.lesson.model.Chapter;
import com.example.cake.lesson.model.Lesson;
import com.example.cake.lesson.model.UserProgress;
import com.example.cake.lesson.repository.ChapterRepository;
import com.example.cake.lesson.repository.LessonRepository;
import com.example.cake.lesson.repository.UserProgressRepository;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class CurriculumFacade {

    private final ChapterService chapterService;
    private final LessonService lessonService;
    private final ProgressService progressService;


    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final UserProgressRepository userProgressRepository;



    public ResponseMessage<List<Chapter>> getCourseChaptersPublic(String courseId) {
        return chapterService.getChaptersByCourse(courseId);
    }

    public ResponseMessage<Chapter> getChapterByIdPublic(String chapterId) {
        return chapterService.getChapterById(chapterId);
    }

    public ResponseMessage<List<Lesson>> getChapterLessonsPublic(String chapterId) {
        return lessonService.getLessonsByChapter(chapterId);
    }

    public ResponseMessage<List<Lesson>> getFullCurriculumPublic(String courseId) {
        return lessonService.getLessonsByCourse(courseId);
    }



    public ResponseMessage<CurriculumUserViewDTO> getCurriculumForUser(String userId, String courseId) {
        // Load progress (optional, user may not be enrolled)
        UserProgress progress = userProgressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderAsc(courseId);

        // Keep stable chapter views with mutable lesson list for grouping
        Map<String, CurriculumUserViewDTO.ChapterView> chapterViewsById = new LinkedHashMap<>();
        for (Chapter c : chapters) {
            chapterViewsById.put(c.getId(), CurriculumUserViewDTO.ChapterView.builder()
                    .chapterId(c.getId())
                    .title(c.getTitle())
                    .order(c.getOrder())
                    .lessons(new ArrayList<>())
                    .build());
        }


        for (Lesson l : lessons) {
            CurriculumUserViewDTO.LessonView lessonView = toLessonView(userId, l, progress);
            CurriculumUserViewDTO.ChapterView ch = chapterViewsById.get(l.getChapterId());
            if (ch != null) {
                ch.getLessons().add(lessonView);
            }
        }

        CurriculumUserViewDTO dto = CurriculumUserViewDTO.builder()
                .courseId(courseId)
                .chapters(new ArrayList<>(chapterViewsById.values()))
                .build();

        return new ResponseMessage<>(true, "Success", dto);
    }

    private CurriculumUserViewDTO.LessonView toLessonView(String userId, Lesson lesson, UserProgress progress) {
        boolean unlocked;
        if (progress == null) {
            // Not enrolled: only free lessons are accessible
            unlocked = Boolean.TRUE.equals(lesson.getIsFree());
        } else {
            ResponseMessage<Boolean> access = progressService.canAccessLesson(userId, lesson.getId());
            unlocked = Boolean.TRUE.equals(access.getData());
        }

        boolean completed = progress != null && progress.isLessonCompleted(lesson.getId());
        Integer videoProgress = 0;
        Boolean quizPassed = null;
        Integer quizScore = null;

        if (progress != null && progress.getLessonsProgress() != null) {
            UserProgress.LessonProgress lp = progress.getLessonsProgress().stream()
                    .filter(x -> lesson.getId().equals(x.getLessonId()))
                    .findFirst()
                    .orElse(null);
            if (lp != null) {
                videoProgress = lp.getVideoProgress() != null ? lp.getVideoProgress() : 0;
                quizPassed = lp.getQuizPassedAt() != null;
                quizScore = lp.getQuizScore();
            }
        }

        return CurriculumUserViewDTO.LessonView.builder()
                .lessonId(lesson.getId())
                .title(lesson.getTitle())
                .order(lesson.getOrder())
                .duration(lesson.getDuration())
                .hasQuiz(lesson.getHasQuiz())
                .unlocked(unlocked)
                .completed(completed)
                .videoProgress(videoProgress)
                .quizPassed(quizPassed)
                .quizScore(quizScore)
                .build();
    }
}
