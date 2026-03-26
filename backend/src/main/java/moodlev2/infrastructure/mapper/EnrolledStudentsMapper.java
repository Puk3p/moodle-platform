package moodlev2.infrastructure.mapper;

import java.util.List;
import java.util.Random;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.EnrollmentEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.course.dto.students.EnrolledStudentsResponse;
import moodlev2.web.course.dto.students.StudentDto;
import moodlev2.web.course.dto.students.StudentStatsDto;
import org.springframework.stereotype.Component;

@Component
public class EnrolledStudentsMapper {

    private static final Random RANDOM = new Random();

    private final List<String> AVATAR_COLORS =
            List.of("#eff6ff", "#fdf2f8", "#ecfdf5", "#fffbeb", "#f3e8ff", "#ecfeff");

    public EnrolledStudentsResponse toDto(CourseEntity course, List<EnrollmentEntity> enrollments) {

        List<StudentDto> students = enrollments.stream().map(e -> mapStudent(e.getUser())).toList();

        long total = students.size();
        long active = students.size() > 0 ? 95 : 0;
        long pending = 0;

        return new EnrolledStudentsResponse(
                course.getCode(), new StudentStatsDto(total, active, pending), students);
    }

    private StudentDto mapStudent(UserEntity user) {
        int colorIndex = (int) (user.getId() % AVATAR_COLORS.size());
        String color = AVATAR_COLORS.get(colorIndex);

        int progress = (int) (user.getId() % 40) + 60;
        String lastActivity = "Today";

        String groupName = (user.getClazz() != null) ? user.getClazz().getName() : "No Group";

        return new StudentDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                groupName,
                progress,
                lastActivity,
                color);
    }
}
