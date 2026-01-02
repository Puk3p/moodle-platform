package moodlev2.web.resource.dto;

import java.util.List;

public record TeacherAssignmentOverviewDto(
        Long assignmentId,
        String title,
        String courseCode,
        List<StudentSubmissionSummaryDto> students
) {}