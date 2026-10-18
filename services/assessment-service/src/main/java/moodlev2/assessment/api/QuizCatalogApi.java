package moodlev2.assessment.api;

import java.util.List;

/** Read access to a course's quizzes, for screens that list them next to course content. */
public interface QuizCatalogApi {

    /**
     * The course's published quizzes, oldest first.
     *
     * @param viewerId whose attempts decide {@link CourseQuiz#canAttempt()}; {@code null} when no
     *     one in particular is looking (always {@code true} then)
     */
    List<CourseQuiz> publishedQuizzes(long courseId, Long viewerId);

    /**
     * @param moduleId the course module it is filed under, {@code null} for none
     * @param canAttempt the viewer has not used up the quiz's attempts
     * @param durationMinutes the time limit, {@code null} or 0 when untimed
     */
    record CourseQuiz(
            long id,
            String title,
            Long moduleId,
            boolean canAttempt,
            int questionsCount,
            Integer durationMinutes,
            boolean passwordProtected) {}
}
