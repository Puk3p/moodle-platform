package moodlev2.academic.application.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.clients.DirectoryApi;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.shared.ColorUtil;
import moodlev2.academic.web.course.dto.ActiveCourseDto;
import moodlev2.academic.web.course.dto.CompletedCourseDto;
import moodlev2.academic.web.course.dto.DashboardHomeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyCoursesService {

    private final DirectoryApi directory;
    private final TeacherNames teacherNames;
    private final CourseRepository courseRepository;
    private final CourseFeed courseFeed;

    @Transactional(readOnly = true)
    public DashboardHomeResponse getDashboardForUserEmail(String email) {
        String userName = "Student";
        UserSummary user = email != null ? directory.findUserByEmail(email).orElse(null) : null;
        if (user != null) {
            userName = user.firstName();
        }

        List<CourseEntity> courseEntities =
                user != null
                        ? courseRepository.findAllCoursesForStudent(user.id(), user.classId())
                        : new ArrayList<>();
        Map<Long, String> instructors = teacherNames.of(courseEntities);

        List<ActiveCourseDto> activeCourses =
                courseEntities.stream().map(c -> mapToActiveCourseDto(c, instructors)).toList();

        CourseFeed.Feed feed = courseFeed.feedFor(user, courseEntities);

        List<CompletedCourseDto> completedCourses = new ArrayList<>();

        return new DashboardHomeResponse(
                userName, activeCourses, completedCourses, feed.deadlines(), feed.activities());
    }

    private ActiveCourseDto mapToActiveCourseDto(CourseEntity c, Map<Long, String> instructors) {
        String instructorName = instructors.getOrDefault(c.getTeacherId(), "Unknown");

        String color = ColorUtil.randomPastelColor();

        return new ActiveCourseDto(
                c.getCode(),
                c.getName(),
                instructorName,
                0, // TODO: Calculează progres real
                color);
    }
}
