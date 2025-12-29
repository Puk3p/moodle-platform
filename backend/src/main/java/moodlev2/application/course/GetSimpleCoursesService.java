package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.web.course.dto.SimpleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSimpleCoursesService {
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<SimpleDto> getCoursesForDropdown() {
        return courseRepository.findAll().stream()
                .map(c -> new SimpleDto(
                        c.getId(),
                        c.getName() + " (" + c.getCode() + ")"
                ))
                .toList();
    }
}