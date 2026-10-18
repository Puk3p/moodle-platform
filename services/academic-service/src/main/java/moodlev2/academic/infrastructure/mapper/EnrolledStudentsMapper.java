package moodlev2.academic.infrastructure.mapper;

import java.util.List;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.students.EnrolledStudentsResponse;
import moodlev2.academic.web.course.dto.students.StudentDto;
import moodlev2.academic.web.course.dto.students.StudentStatsDto;
import org.springframework.stereotype.Component;

@Component
public class EnrolledStudentsMapper {

    private final List<String> AVATAR_COLORS =
            List.of("#eff6ff", "#fdf2f8", "#ecfdf5", "#fffbeb", "#f3e8ff", "#ecfeff");

    public EnrolledStudentsResponse toDto(CourseEntity course, List<UserSummary> members) {
        List<StudentDto> students = members.stream().map(this::mapStudent).toList();

        long total = students.size();
        long active = students.size() > 0 ? 95 : 0;
        long pending = 0;

        return new EnrolledStudentsResponse(
                course.getCode(), new StudentStatsDto(total, active, pending), students);
    }

    private StudentDto mapStudent(UserSummary user) {
        int colorIndex = (int) (user.id() % AVATAR_COLORS.size());
        String color = AVATAR_COLORS.get(colorIndex);

        int progress = (int) (user.id() % 40) + 60;
        String lastActivity = "Today";
        String groupName = (user.className() != null) ? user.className() : "No Group";

        return new StudentDto(
                user.id(),
                user.firstName(),
                user.lastName(),
                user.email(),
                groupName,
                progress,
                lastActivity,
                color);
    }
}
