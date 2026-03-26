package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.CourseEditMapper;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.edit.CourseEditDto;
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
