package moodlev2.academic.api;

import java.util.List;
import moodlev2.identity.api.UserSummary;

/**
 * Who belongs to a course. The academic module owns enrollments and the classes a course is
 * assigned to; other modules ask here instead of reading them.
 *
 * <p>A student belongs to a course when enrolled directly or placed in a class the course is
 * assigned to. This is the one definition of membership in the application.
 */
public interface MembershipApi {

    /**
     * Staff (teachers and admins) belong to every course; anyone else by the rule above. False for
     * a course that does not exist.
     */
    boolean isMember(long courseId, UserSummary user);

    /**
     * Everyone in the course: direct enrollments first, then members of assigned classes. Empty for
     * a course that does not exist.
     */
    List<UserSummary> studentsOf(long courseId);

    /** Courses the user belongs to as a student, by the rule above (no staff shortcut). */
    List<CourseSummary> coursesOfStudent(UserSummary user);

    /** Courses whose assigned teacher is this account. */
    List<CourseSummary> coursesTaughtBy(long teacherId);

    /** Courses the user teaches or belongs to as a student, without duplicates. */
    List<CourseSummary> coursesInvolving(UserSummary user);
}
