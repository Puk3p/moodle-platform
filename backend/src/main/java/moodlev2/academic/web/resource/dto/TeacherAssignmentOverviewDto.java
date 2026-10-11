package moodlev2.academic.web.resource.dto;

import java.util.List;

public record TeacherAssignmentOverviewDto(
        Long assignmentId,
        String title,
        String courseCode,
        List<StudentSubmissionSummaryDto> students) {}
