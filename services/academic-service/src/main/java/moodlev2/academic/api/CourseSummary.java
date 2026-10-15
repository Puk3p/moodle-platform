package moodlev2.academic.api;

/**
 * What other modules may know about a course. Other modules keep only {@link #id()} and look the
 * rest up through {@link CourseCatalogApi}.
 *
 * @param teacherId the teaching account, {@code null} when none is assigned
 */
public record CourseSummary(
        long id,
        String code,
        String name,
        String term,
        String status,
        Long teacherId,
        String imageUrl) {}
