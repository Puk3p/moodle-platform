package moodlev2.web.admin;

import lombok.RequiredArgsConstructor;
import moodlev2.application.classs.ClassService;
import moodlev2.web.admin.dto.CreateClassRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClassesController {
    private final ClassService classService;

    @PostMapping
    public void createClass(@RequestBody CreateClassRequest request) {
        classService.createClass(request);
    }
}
