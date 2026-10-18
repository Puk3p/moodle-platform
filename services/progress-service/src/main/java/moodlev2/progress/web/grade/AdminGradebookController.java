package moodlev2.progress.web.grade;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.progress.application.grade.AdminGradebookService;
import moodlev2.progress.web.grade.dto.AdminGradeDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/grades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGradebookController {

    private final AdminGradebookService service;

    @GetMapping
    public List<AdminGradeDto> getAllGrades() {
        return service.getAllGrades();
    }
}
