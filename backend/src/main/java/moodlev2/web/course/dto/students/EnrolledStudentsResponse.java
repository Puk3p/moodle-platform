package moodlev2.web.course.dto.students;

import java.util.List;

public record EnrolledStudentsResponse(
        String courseCode, StudentStatsDto stats, List<StudentDto> students) {}
