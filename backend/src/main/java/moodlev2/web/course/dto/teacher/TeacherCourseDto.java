package moodlev2.web.course.dto.teacher;

public record TeacherCourseDto(
        String code,
        String title,
        long studentsCount,
        int modulesCount,
        String term,
        String status,
        Double avgGrade,
        Integer pendingSubmissions,
        boolean isStarted) {}
