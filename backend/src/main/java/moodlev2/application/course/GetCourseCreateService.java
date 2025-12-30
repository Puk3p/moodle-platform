package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.course.dto.CreateCourseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GetCourseCreateService {

    private final CourseRepository courseRepository;
    private final SpringDataUserRepository userRepository;

    @Transactional
    public void createCourse(CreateCourseDto dto) {

        if (dto.teacherId() == null) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        UserEntity teacher = userRepository.findById(dto.teacherId())
                .orElseThrow(() -> new NotFoundException("Teacher not found with ID: " + dto.teacherId()));

        CourseEntity course = new CourseEntity();
        course.setCode(dto.code());
        course.setName(dto.title());
        course.setTerm(dto.term() != null ? dto.term() : "Fall 2024");
        course.setDescription(dto.description());

        course.setTeacher(teacher);

        course.setStatus("DRAFT");

        course.setModules(new ArrayList<>());
        course.setEnrollments(new ArrayList<>());

        courseRepository.save(course);
    }
}