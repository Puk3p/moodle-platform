package moodlev2.academic.directory;

import java.util.Set;

/**
 * What courses may know about an account, as identity-service reports it. A snapshot: courses refer
 * to people by {@link #id()} only and look the rest up through {@link DirectoryApi}.
 *
 * @param classId the student's class, or {@code null} when not placed in one
 * @param className the class's name, or {@code null} with {@code classId}
 */
public record UserSummary(
        long id,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles,
        Long classId,
        String className,
        boolean active) {

    public UserSummary {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** Teachers and admins manage every course. */
    public boolean isStaff() {
        return roles.contains(Role.TEACHER) || roles.contains(Role.ADMIN);
    }

    /** A user holding TEACHER counts as a teacher even if also a student. */
    public boolean isTeacher() {
        return roles.contains(Role.TEACHER);
    }

    public boolean isStudent() {
        return roles.contains(Role.STUDENT) && !isTeacher();
    }
}
