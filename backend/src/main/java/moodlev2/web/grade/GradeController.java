package moodlev2.web.grade;

import lombok.RequiredArgsConstructor;
import moodlev2.application.grade.GetGradesPageService;
import moodlev2.web.grade.dto.GradesPageResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GetGradesPageService getGradesPageService;

    @GetMapping
    public GradesPageResponse getGradesPage(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getGradesPageService.getGradesPageForUser(email);
    }
}