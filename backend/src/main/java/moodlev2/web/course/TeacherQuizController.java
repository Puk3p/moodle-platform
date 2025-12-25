package moodlev2.web.course;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.GetTeacherQuizzesService;
import moodlev2.web.course.dto.teacher.TeacherQuizDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/quizzes")
@RequiredArgsConstructor
public class TeacherQuizController {

    private final GetTeacherQuizzesService getTeacherQuizzesService;

    @GetMapping
    public List<TeacherQuizDto> getQuizzes(Authentication authentication) {
        return getTeacherQuizzesService.getQuizzesForTeacher(authentication.getName());
    }
}