package moodlev2.messaging.clients;

import java.util.List;

/** Who belongs to which course, owned by academic-service. */
public interface Membership {

    /** Courses the student belongs to (enrolled directly or through their class). */
    List<Course> coursesOfStudent(long userId);

    List<Course> coursesTaughtBy(long teacherId);

    /** Everyone in the course. */
    List<Person> studentsOf(long courseId);
}
