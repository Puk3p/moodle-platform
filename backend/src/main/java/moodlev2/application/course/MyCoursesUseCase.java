// application/course/MyCoursesUseCase.java
package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.web.course.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyCoursesUseCase {

    private final UserRepositoryPort userRepository;

    public DashboardHomeResponse getDashboardForUserEmail(String email) {
        String userName = "Student";

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null && user.getFirstName() != null && !user.getFirstName().isBlank()) {
                userName = user.getFirstName();
            }
        }

        List<ActiveCourseDto> activeCourses = List.of(
                new ActiveCourseDto("CS201", "Data Structuresssssssss", "Prof. Eleanor Vance", 75, "#CADFD1"),
                new ActiveCourseDto("CS350", "Operating Systems", "Dr. Ben Carter", 40, "#C5DED7"),
                new ActiveCourseDto("CS110", "Intro to Programming", "Prof. Ada Lovelace", 95, "#213C2F"),
                new ActiveCourseDto("INFO420", "Project Management", "Dr. Ian Malcolm", 60, "#F4F2F0")
        );

        List<CompletedCourseDto> completedCourses = List.of(
                new CompletedCourseDto("CS101", "Intro to Computer Science", "Dec 15, 2023"),
                new CompletedCourseDto("MATH251", "Linear Algebra", "Dec 18, 2023")
        );

        List<DeadlineDto> deadlines = List.of(
                new DeadlineDto("Lab 4 Submission", "CS201: Data Structures", "Due in 2 days"),
                new DeadlineDto("Mid-term Quiz", "CS350: Operating Systems", "Due in 5 days"),
                new DeadlineDto("Project Proposal", "INFO420: Project Management", "Due in 7 days")
        );

        List<ActivityDto> activities = List.of(
                new ActivityDto("New announcement in CS350", "2 hours ago"),
                new ActivityDto("Assignment graded in CS110", "1 day ago"),
                new ActivityDto("New post in CS201 discussion forum", "3 days ago")
        );

        return new DashboardHomeResponse(
                userName,
                activeCourses,
                completedCourses,
                deadlines,
                activities
        );
    }
}
