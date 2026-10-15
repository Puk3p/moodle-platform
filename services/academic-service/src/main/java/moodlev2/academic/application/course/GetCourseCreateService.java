package moodlev2.academic.application.course;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.directory.DirectoryApi;
import moodlev2.academic.directory.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.shared.exception.NotFoundException;
import moodlev2.academic.web.course.dto.CreateCourseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseCreateService {

    private final CourseRepository courseRepository;
    private final DirectoryApi directory;

    @Transactional
    public void createCourse(CreateCourseDto dto) {

        if (dto.teacherId() == null) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        UserSummary teacher =
                directory
                        .findUser(dto.teacherId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Teacher not found with ID: " + dto.teacherId()));

        CourseEntity course = new CourseEntity();
        course.setCode(dto.code());
        course.setName(dto.title());
        course.setTerm(dto.term() != null ? dto.term() : "Fall 2024");
        course.setDescription(dto.description());

        course.setTeacherId(teacher.id());

        course.setStatus("DRAFT");

        course.setModules(new ArrayList<>());
        course.setEnrollments(new ArrayList<>());

        courseRepository.save(course);
    }
}
