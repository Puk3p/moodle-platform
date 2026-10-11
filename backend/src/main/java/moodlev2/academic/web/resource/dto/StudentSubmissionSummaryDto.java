package moodlev2.academic.web.resource.dto;

import java.time.LocalDateTime;

public record StudentSubmissionSummaryDto(
        Long studentId,
        String studentName,
        String email,
        String avatarColor,
        String status,
        Integer grade,
        Long submissionId,
        LocalDateTime submittedAt) {}
