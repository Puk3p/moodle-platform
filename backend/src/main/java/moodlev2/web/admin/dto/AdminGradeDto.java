package moodlev2.web.admin.dto;

import java.math.BigDecimal;

public record AdminGradeDto(
        Long gradeId,
        String courseCode,
        String courseName,
        String studentName,
        String studentAvatar,
        String itemName,
        String submittedAt,
        String scoreLabel,
        BigDecimal scoreValue,
        String teacherName) {}
