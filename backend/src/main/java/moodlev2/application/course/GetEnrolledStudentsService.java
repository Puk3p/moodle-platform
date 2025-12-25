package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.EnrolledStudentsMapper;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.students.EnrolledStudentsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetEnrolledStudentsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrolledStudentsMapper mapper;

    @Transactional(readOnly = true)
    public EnrolledStudentsResponse getEnrolledStudents(String courseCode) {
        CourseEntity course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new NotFoundException("Course not found"));

        var enrollments = enrollmentRepository.findAllByCourseCode(courseCode);

        return mapper.toDto(course, enrollments);
    }
}