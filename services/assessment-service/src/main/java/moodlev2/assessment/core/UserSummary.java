package moodlev2.assessment.core;

import java.util.Set;

/**
 * An account, as core's directory describes it. Assessment keeps only user ids and looks the rest
 * up when it needs it.
 *
 * @param roles role names: STUDENT, TEACHER, ADMIN
 * @param classId the student's class, {@code null} when not placed in one
 */
public record UserSummary(
        long id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        Long classId,
        String className,
        boolean active) {

    public UserSummary {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** Teachers and admins manage every course and may preview any quiz. */
    public boolean isStaff() {
        return roles.contains("TEACHER") || roles.contains("ADMIN");
    }
}
