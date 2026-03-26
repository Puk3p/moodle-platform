package moodlev2.web.course;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.GetTeacherDashboardService;
import moodlev2.web.course.dto.teacher.TeacherDashboardResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final GetTeacherDashboardService dashboardService;

    @GetMapping("/dashboard")
    public TeacherDashboardResponse getDashboard(Authentication authentication) {
        return dashboardService.getTeacherDashboard(authentication.getName());
    }
}
