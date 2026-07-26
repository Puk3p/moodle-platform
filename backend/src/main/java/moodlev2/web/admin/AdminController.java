package moodlev2.web.admin;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CourseRepository courseRepository;

    @PostMapping("/courses")
    public CourseEntity createCourse(@RequestBody CourseEntity course) {
        return courseRepository.save(course);
    }
}
