package moodlev2.academic.api;

import java.util.List;
import moodlev2.identity.api.UserSummary;

/**
 * Who belongs to a course. The academic module owns enrollments and the classes a course is
 * assigned to; other modules ask here instead of reading them.
 */
public interface MembershipApi {

    /**
     * Staff (teachers and admins) belong to every course; a student when enrolled directly or
     * placed in a class the course is assigned to. False for a course that does not exist.
     */
    boolean isMember(long courseId, UserSummary user);

    /**
     * Everyone in the course: direct enrollments first, then members of assigned classes. Empty for
     * a course that does not exist.
     */
    List<UserSummary> studentsOf(long courseId);
}
