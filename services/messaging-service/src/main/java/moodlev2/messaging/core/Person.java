package moodlev2.messaging.core;

import java.util.Set;

/**
 * An account, as core's directory describes it. Messaging stores none of this; it looks people up
 * when it needs them.
 *
 * @param roles role names: STUDENT, TEACHER, ADMIN
 */
public record Person(
        long id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        Long classId,
        String className,
        boolean active) {

    public Person {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** A user holding TEACHER counts as a teacher even if also a student. */
    public boolean isTeacher() {
        return roles.contains("TEACHER");
    }

    public boolean isStudent() {
        return roles.contains("STUDENT") && !isTeacher();
    }
}
