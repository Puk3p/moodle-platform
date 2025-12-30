package moodlev2.application.admin;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.GradeRepository;
import moodlev2.infrastructure.persistence.jpa.entity.GradeEntity;
import moodlev2.web.admin.dto.AdminGradeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGradebookService {

    private final GradeRepository gradeRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    @Transactional(readOnly = true)
    public List<AdminGradeDto> getAllGrades() {
        return gradeRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    private AdminGradeDto mapToDto(GradeEntity g) {
        String studentName = g.getUser().getFirstName() + " " + g.getUser().getLastName();
        String teacherName = (g.getCourse().getTeacher() != null)
                ? g.getCourse().getTeacher().getLastName()
                : "Unknown";

        String courseCode = g.getCourse().getCode();
        String courseName = g.getCourse().getName();

        double max = g.getMaxScore() != null ? g.getMaxScore().doubleValue() : 100.0;
        double score = g.getScoreReceived() != null ? g.getScoreReceived().doubleValue() : 0.0;
        int percent = (max > 0) ? (int)((score / max) * 100) : 0;

        String scoreLabel = String.format("%d%% - %s/%s", percent, g.getScoreReceived(), g.getMaxScore());
        String dateStr = g.getGradedAt() != null ? g.getGradedAt().toString() : "N/A";

        return new AdminGradeDto(
                g.getId(),
                courseCode,
                courseName,
                studentName,
                null,
                g.getItemName(),
                dateStr,
                scoreLabel,
                g.getScoreReceived(),
                teacherName
        );
    }
}