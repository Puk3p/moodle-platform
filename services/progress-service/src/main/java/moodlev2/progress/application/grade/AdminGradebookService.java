package moodlev2.progress.application.grade;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.progress.clients.Course;
import moodlev2.progress.clients.Courses;
import moodlev2.progress.clients.Directory;
import moodlev2.progress.clients.Person;
import moodlev2.progress.infrastructure.persistence.GradeRepository;
import moodlev2.progress.infrastructure.persistence.entity.GradeEntity;
import moodlev2.progress.web.grade.dto.AdminGradeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminGradebookService {

    private final GradeRepository gradeRepository;
    private final Directory directory;
    private final Courses catalog;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    @Transactional(readOnly = true)
    public List<AdminGradeDto> getAllGrades() {
        List<GradeEntity> grades = gradeRepository.findAll();
        Map<Long, Course> courses =
                catalog
                        .findCourses(
                                grades.stream()
                                        .map(GradeEntity::getCourseId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Course::id, Function.identity()));
        Set<Long> people = new HashSet<>();
        for (GradeEntity g : grades) {
            people.add(g.getUserId());
        }
        courses.values().stream()
                .map(Course::teacherId)
                .filter(java.util.Objects::nonNull)
                .forEach(people::add);
        Map<Long, Person> users =
                directory.findUsers(people).stream()
                        .collect(Collectors.toMap(Person::id, Function.identity()));
        return grades.stream().map(g -> mapToDto(g, courses.get(g.getCourseId()), users)).toList();
    }

    private AdminGradeDto mapToDto(GradeEntity g, Course course, Map<Long, Person> users) {
        Person student = users.get(g.getUserId());
        Person teacher = course == null ? null : users.get(course.teacherId());
        String studentName = student != null ? student.fullName() : "Deleted user";
        String teacherName = teacher != null ? teacher.lastName() : "Unknown";

        String courseCode = course != null ? course.code() : "";
        String courseName = course != null ? course.name() : "";

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
