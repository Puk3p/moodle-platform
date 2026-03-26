package moodlev2.web.resource.dto;

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
