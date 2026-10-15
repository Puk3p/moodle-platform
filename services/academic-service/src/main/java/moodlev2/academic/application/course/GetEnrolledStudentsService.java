package moodlev2.academic.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.mapper.EnrolledStudentsMapper;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.shared.exception.NotFoundException;
import moodlev2.academic.web.course.dto.students.EnrolledStudentsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetEnrolledStudentsService {

    private final CourseRepository courseRepository;
    private final CourseRoster roster;
    private final EnrolledStudentsMapper mapper;

    @Transactional(readOnly = true)
    public EnrolledStudentsResponse getEnrolledStudents(String courseCode) {
        CourseEntity course =
                courseRepository
                        .findByCode(courseCode)
                        .orElseThrow(() -> new NotFoundException("Course not found"));

        return mapper.toDto(course, roster.studentsOf(course));
    }
}
