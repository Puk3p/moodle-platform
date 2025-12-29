package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.CompletedCourseSummaryDto;
import moodlev2.web.course.dto.CourseOverviewDto;
import moodlev2.web.course.dto.CoursesPageResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCoursesPageService {
    private final UserRepositoryPort userRepository;
    private final CourseRepository courseRepository;

    public CoursesPageResponse getCoursesPageForUser(String email) {

        String userName = "Guest";
        String avatar = "https://ui-avatars.com/api/?background=random";

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userName = user.getFirstName() + " " + user.getLastName();
                avatar = "https://ui-avatars.com/api/?name=" + userName + "&background=0D8ABC&color=fff";
            }
        }

        List<CourseEntity> courseEntities = courseRepository.findAllByUserEmail(email);

        List<CourseOverviewDto> activeCourses = courseEntities.stream()
                .map(c -> {
                    String instructorName = (c.getTeacher() != null)
                            ? c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName()
                            : "Unknown Instructor";

                    return new CourseOverviewDto(
                            c.getCode().toLowerCase(),
                            c.getCode(),
                            c.getName(),
                            instructorName,
                            0, // TODO:calcula progresul real din note/module!!!nu i gata
                            "Check calendar", //TODO: Cel mai apropiat deadline!!!! nu i gata
                            c.getImageUrl()
                    );
                })
                .toList();

        List<CompletedCourseSummaryDto> completedCourses = new ArrayList<>();

        return new CoursesPageResponse(
                userName,
                "Student",
                avatar,
                activeCourses,
                completedCourses
        );
    }
}