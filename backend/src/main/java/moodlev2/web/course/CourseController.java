// web/course/CourseController.java
package moodlev2.web.course;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.MyCoursesUseCase;
import moodlev2.web.course.dto.DashboardHomeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final MyCoursesUseCase myCoursesUseCase;

    @GetMapping("/my-dashboard")
    public DashboardHomeResponse getMyDashboard(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return myCoursesUseCase.getDashboardForUserEmail(email);
    }
}
