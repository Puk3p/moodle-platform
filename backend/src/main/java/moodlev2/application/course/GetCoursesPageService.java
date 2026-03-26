package moodlev2.application.course;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.web.course.dto.CompletedCourseSummaryDto;
import moodlev2.web.course.dto.CourseOverviewDto;
import moodlev2.web.course.dto.CoursesPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCoursesPageService {
    private final UserRepositoryPort userRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CoursesPageResponse getCoursesPageForUser(String email) {

        String userName = "Guest";
        String avatar = "https://ui-avatars.com/api/?background=random";
        Long userId = null;

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userName = user.getFirstName() + " " + user.getLastName();
                avatar =
                        "https://ui-avatars.com/api/?name="
                                + userName
                                + "&background=0D8ABC&color=fff";
                userId = user.getId();
            }
        }

        List<CourseEntity> courseEntities;

        if (userId != null) {
            courseEntities = courseRepository.findAllCoursesForStudent(userId);
        } else {
            courseEntities = new ArrayList<>();
        }

        LocalDate now = LocalDate.now();

        List<CourseOverviewDto> activeCourses =
                courseEntities.stream()
                        .map(
                                c -> {
                                    String instructorName =
                                            (c.getTeacher() != null)
                                                    ? c.getTeacher().getFirstName()
                                                            + " "
                                                            + c.getTeacher().getLastName()
                                                    : "Unknown Instructor";

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
