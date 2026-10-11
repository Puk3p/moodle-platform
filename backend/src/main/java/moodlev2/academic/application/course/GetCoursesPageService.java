package moodlev2.academic.application.course;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseModuleEntity;
import moodlev2.academic.web.course.dto.CompletedCourseSummaryDto;
import moodlev2.academic.web.course.dto.CourseOverviewDto;
import moodlev2.academic.web.course.dto.CoursesPageResponse;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCoursesPageService {
    private final DirectoryApi directory;
    private final TeacherNames teacherNames;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CoursesPageResponse getCoursesPageForUser(String email) {

        String userName = "Guest";
        String avatar = "https://ui-avatars.com/api/?background=random";
        UserSummary user = email != null ? directory.findUserByEmail(email).orElse(null) : null;
        if (user != null) {
            userName = user.fullName();
            avatar =
                    "https://ui-avatars.com/api/?name=" + userName + "&background=0D8ABC&color=fff";
        }

        List<CourseEntity> courseEntities =
                user != null
                        ? courseRepository.findAllCoursesForStudent(user.id(), user.classId())
                        : new ArrayList<>();
        Map<Long, String> instructors = teacherNames.of(courseEntities);

        LocalDate now = LocalDate.now();

        List<CourseOverviewDto> activeCourses =
                courseEntities.stream()
                        .map(
                                c -> {
                                    String instructorName =
                                            instructors.getOrDefault(
                                                    c.getTeacherId(), "Unknown Instructor");

                                    int progress = calculateProgress(c, now);

                                    return new CourseOverviewDto(
                                            String.valueOf(c.getId()),
                                            c.getCode(),
                                            c.getName(),
                                            instructorName,
                                            progress,
                                            "Check calendar",
                                            c.getImageUrl());
                                })
                        .toList();

        List<CompletedCourseSummaryDto> completedCourses = new ArrayList<>();

        return new CoursesPageResponse(
                userName, "Student", avatar, activeCourses, completedCourses);
    }

    private int calculateProgress(CourseEntity course, LocalDate now) {
        if (course.getModules() == null || course.getModules().isEmpty()) {
            return 0;
        }

        long totalModules = course.getModules().size();
        long passedModules =
                course.getModules().stream().filter(m -> isModulePassed(m, now)).count();

        if (totalModules == 0) return 0;

        return (int) ((passedModules * 100) / totalModules);
    }

    private boolean isModulePassed(CourseModuleEntity module, LocalDate now) {
        if (module.getEndDate() != null) {
            return module.getEndDate().isBefore(now);
        }

        if (module.getStartDate() != null) {
            return module.getStartDate().plusDays(7).isBefore(now);
        }
        return false;
    }
}
