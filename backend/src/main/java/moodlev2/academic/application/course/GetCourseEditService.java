package moodlev2.academic.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.mapper.CourseEditMapper;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.edit.CourseEditDto;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseEditService {

    private final CourseRepository courseRepository;
    private final CourseEditMapper mapper;

    @Transactional(readOnly = true)
    public CourseEditDto getCourseForEdit(String code) {
        CourseEntity course =
                courseRepository
                        .findByCode(code)
                        .orElseThrow(() -> new NotFoundException("Course not found"));

        return mapper.toDto(course);
    }
}
