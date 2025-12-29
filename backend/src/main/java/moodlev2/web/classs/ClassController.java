package moodlev2.web.classs;

import lombok.RequiredArgsConstructor;
import moodlev2.application.classs.GetSimpleClassesService;
import moodlev2.web.course.dto.SimpleDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final GetSimpleClassesService getSimpleClassesService;

    @GetMapping("/list")
    public List<SimpleDto> getClassesForDropdown() {
        return getSimpleClassesService.getClassesForDropdown();
    }
}