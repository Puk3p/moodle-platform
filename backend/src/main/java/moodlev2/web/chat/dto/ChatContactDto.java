package moodlev2.web.chat.dto;

import java.util.List;

/**
 * Someone the caller may message: a student's teachers, or a teacher's students, plus anyone they
 * already have a conversation with.
 *
 * @param role "TEACHER" or "STUDENT"
 * @param courses codes of the courses the two share, for context in the contact list
 */
public record ChatContactDto(
        String email, String firstName, String lastName, String role, List<String> courses) {}
