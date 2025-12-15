// web/course/CourseController.java
package moodlev2.web.course;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.GetCourseDetailsService;
import moodlev2.application.course.GetCoursesPageService;
import moodlev2.application.course.MyCoursesService;
import moodlev2.web.course.dto.CourseDetailsResponse;
import moodlev2.web.course.dto.CoursesPageResponse;
import moodlev2.web.course.dto.DashboardHomeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final MyCoursesService myCoursesUseCase;
    private final GetCoursesPageService getCoursesPageService;
    private final GetCourseDetailsService getCourseDetailsUseCase;

    @GetMapping("/my-dashboard")
    public DashboardHomeResponse getMyDashboard(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return myCoursesUseCase.getDashboardForUserEmail(email);
    }

    @GetMapping
    public CoursesPageResponse getAllCoursesPage(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getCoursesPageService.getCoursesPageForUser(email);
    }

    @GetMapping("/{id}")
    public CourseDetailsResponse getCourseDetails(@PathVariable String id) {
        return getCourseDetailsUseCase.getCourseDetails(id);
    }
}
