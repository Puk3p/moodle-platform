package moodlev2.progress.clients;

import java.util.Collection;
import java.util.List;

/** Courses and who belongs to them, owned by academic-service. */
public interface Courses {

    List<Course> findCourses(Collection<Long> ids);

    /** Courses the user belongs to as a student (enrolled directly or through their class). */
    List<Course> coursesOfStudent(long userId);

    /** Courses the user teaches or belongs to. */
    List<Course> coursesInvolving(long userId);
}
