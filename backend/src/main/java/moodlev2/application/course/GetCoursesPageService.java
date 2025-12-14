package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.web.course.dto.CompletedCourseSummaryDto;
import moodlev2.web.course.dto.CourseOverviewDto;
import moodlev2.web.course.dto.CoursesPageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCoursesPageService {
    private final UserRepositoryPort userRepository;

    public CoursesPageResponse getCoursesPageForUser(String email) {
        String userName = "Guest";
        String userRole = "Student";
        String avatar = "https://ui-avatars.com/api/?name=Guest+User&background=random";

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userName = user.getFirstName() + " " + user.getLastName();
                avatar = "https://ui-avatars.com/api/?name=" + user.getFirstName() + "+" + user.getLastName() + "&background=0D8ABC&color=fff";
            }
        }

        List<CourseOverviewDto> activeCourses = List.of(
                new CourseOverviewDto("cs201", "CS201", "Data Structures", "Prof. Eleanor Vance", 75, "Lab 4 in 2 days", "https://img.freepik.com/free-vector/gradient-abstract-background_23-2149121815.jpg"),
                new CourseOverviewDto("cs350", "CS350", "Operating Systems", "Dr. Ben Carter", 40, "Mid-term in 5 days", "https://img.freepik.com/free-vector/clean-gradient-background_23-2149132549.jpg"),
                new CourseOverviewDto("cs110", "CS110", "Intro to Programming", "Prof. Ada Lovelace", 95, "Final Project in 1 day", "https://img.freepik.com/free-vector/dark-green-background-design_1035-18237.jpg"),
                new CourseOverviewDto("info420", "INFO420", "Project Management", "Dr. Ian Malcolm", 60, "Proposal in 7 days", "https://img.freepik.com/free-photo/white-painted-wall-texture-background_53876-138197.jpg")
        );

        List<CompletedCourseSummaryDto> completedCourses = List.of(
                new CompletedCourseSummaryDto("CS101: Intro to Computer Science", "Dec 15, 2023", "A+"),
                new CompletedCourseSummaryDto("MATH251: Linear Algebra", "Dec 18, 2023", "A-")
        );

        return new CoursesPageResponse(
                userName,
                "Computer Science",
                avatar,
                activeCourses,
                completedCourses
        );
    }
}
