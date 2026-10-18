package moodlev2.academic.clients;

import java.util.List;

/**
 * A course's quizzes, kept by assessment-service, for the screens that list them next to course
 * content. A quiz service that cannot be reached means no quizzes listed, never a failed page.
 */
public interface CourseQuizzes {

    /**
     * Published quizzes of the course, oldest first; empty if unavailable.
     *
     * @param viewerId whose attempts decide {@link Quiz#canAttempt()}; {@code null} for nobody
     */
    List<Quiz> publishedQuizzes(long courseId, Long viewerId);

    /**
     * @param moduleId the module it is filed under, {@code null} for none
     * @param durationMinutes the time limit, {@code null} or 0 when untimed
     */
    record Quiz(
            long id,
            String title,
            Long moduleId,
            boolean canAttempt,
            int questionsCount,
            Integer durationMinutes,
            boolean passwordProtected) {}
}
