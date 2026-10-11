package moodlev2.academic.web.resource.dto;

import java.time.LocalDateTime;

public record MySubmissionDto(
        Long id,
        String textResponse,
        String fileUrl,
        String fileName,
        LocalDateTime submittedAt,
        Integer grade,
        String feedback) {}
