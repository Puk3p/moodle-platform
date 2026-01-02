package moodlev2.web.resource.dto;

import java.time.LocalDateTime;

public record TeacherSubmissionViewDto(
        Long submissionId,
        String studentName,
        String studentEmail,
        String assignmentTitle,
        String courseCode,
        LocalDateTime submittedAt,
        String fileUrl,
        String fileName,
        String textResponse,
        Integer currentGrade,
        Integer maxGrade,
        String currentFeedback,
        String assignmentResourceUrl
) {}