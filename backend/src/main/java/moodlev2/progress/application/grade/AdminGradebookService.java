package moodlev2.progress.application.grade;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.progress.infrastructure.persistence.GradeRepository;
import moodlev2.progress.infrastructure.persistence.entity.GradeEntity;
import moodlev2.progress.web.grade.dto.AdminGradeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminGradebookService {

    private final GradeRepository gradeRepository;
    private final DirectoryApi directory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    @Transactional(readOnly = true)
    public List<AdminGradeDto> getAllGrades() {
        List<GradeEntity> grades = gradeRepository.findAll();
        Set<Long> people = new HashSet<>();
        for (GradeEntity g : grades) {
            people.add(g.getUserId());
            if (g.getCourse().getTeacherId() != null) {
                people.add(g.getCourse().getTeacherId());
            }
        }
        Map<Long, UserSummary> users =
                directory.findUsers(people).stream()
                        .collect(Collectors.toMap(UserSummary::id, Function.identity()));
        return grades.stream().map(g -> mapToDto(g, users)).toList();
    }

    private AdminGradeDto mapToDto(GradeEntity g, Map<Long, UserSummary> users) {
        UserSummary student = users.get(g.getUserId());
        UserSummary teacher = users.get(g.getCourse().getTeacherId());
        String studentName = student != null ? student.fullName() : "Deleted user";
        String teacherName = teacher != null ? teacher.lastName() : "Unknown";

        String courseCode = g.getCourse().getCode();
        String courseName = g.getCourse().getName();

        double max = g.getMaxScore() != null ? g.getMaxScore().doubleValue() : 100.0;
        double score = g.getScoreReceived() != null ? g.getScoreReceived().doubleValue() : 0.0;
        int percent = (max > 0) ? (int) ((score / max) * 100) : 0;

        String scoreLabel =
                String.format("%d%% - %s/%s", percent, g.getScoreReceived(), g.getMaxScore());
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
                teacherName);
    }
}
