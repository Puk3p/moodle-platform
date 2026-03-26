package moodlev2.web.resource.dto;

import java.time.LocalDateTime;

public record StudentAssignmentDetailsDto(
        Long id,
        String title,
        String description,
        LocalDateTime dueDate,
        Integer maxGrade,
        String submissionType,
        String fileUrl,
        MySubmissionDto mySubmission) {}
