package moodlev2.web.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.admin.AdminGradebookService;
import moodlev2.web.admin.dto.AdminGradeDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/grades")
@RequiredArgsConstructor
public class AdminGradebookController {

    private final AdminGradebookService service;

    @GetMapping
    public List<AdminGradeDto> getAllGrades() {
        return service.getAllGrades();
    }
}
