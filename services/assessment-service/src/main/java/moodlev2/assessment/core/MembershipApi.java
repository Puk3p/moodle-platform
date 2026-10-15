package moodlev2.assessment.core;

import java.util.List;

/** Who belongs to which course, owned by core. */
public interface MembershipApi {

    /** Staff belong to every course; a student when enrolled directly or through their class. */
    boolean isMember(long courseId, UserSummary user);

    /** Everyone in the course. */
    List<UserSummary> studentsOf(long courseId);
}
