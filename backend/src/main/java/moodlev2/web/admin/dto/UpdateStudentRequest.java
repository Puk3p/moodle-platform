package moodlev2.web.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sizes follow the users table (first/last name VARCHAR(100), email VARCHAR(255)). */
public record UpdateStudentRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        Long classId) {}
