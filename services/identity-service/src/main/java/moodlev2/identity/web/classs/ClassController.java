package moodlev2.identity.web.classs;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.application.classs.ClassService;
import moodlev2.identity.shared.SimpleDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping("/list")
    public List<SimpleDto> getClassesForDropdown() {
        return classService.getClassesForDropdown();
    }
}
