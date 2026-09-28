package moodlev2.web.user.dto;

import java.util.List;

/**
 * The caller's own account, shaped by role: a student has a class, an ID and enrolled courses; a
 * teacher has the courses they teach. Exactly one of {@code student} / {@code teacher} is set for
 * those roles, and neither for an admin-only account.
 *
 * @param role "TEACHER", "STUDENT" or "ADMIN"; a user holding TEACHER counts as a teacher
 */
public record UserProfileDto(
        String email,
        String firstName,
        String lastName,
        String role,
        boolean twoFaEnabled,
        StudentProfile student,
        TeacherProfile teacher) {

    /**
     * @param className {@code null} when the student has not been placed in a class
     */
    public record StudentProfile(
            String studentId, String className, List<EnrolledCourse> courses) {}

    /**
     * @param teacherName {@code null} when the course has no assigned teacher
     */
    public record EnrolledCourse(String code, String name, String term, String teacherName) {}

    /**
     * @param studentCount distinct students across all taught courses
     */
    public record TeacherProfile(List<TaughtCourse> courses, int studentCount) {}

    public record TaughtCourse(
            String code, String name, String term, String status, int studentCount) {}
}
