package moodlev2.messaging.core;

import java.util.List;

/** Who belongs to which course, owned by core. */
public interface CoreMembership {

    /** Courses the student belongs to (enrolled directly or through their class). */
    List<Course> coursesOfStudent(long userId);

    List<Course> coursesTaughtBy(long teacherId);

    /** Everyone in the course. */
    List<Person> studentsOf(long courseId);
}
