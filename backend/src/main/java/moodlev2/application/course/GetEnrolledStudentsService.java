package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.EnrolledStudentsMapper;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.EnrollmentEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.course.dto.students.EnrolledStudentsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetEnrolledStudentsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SpringDataUserRepository userRepository;
    private final EnrolledStudentsMapper mapper;

    @Transactional(readOnly = true)
    public EnrolledStudentsResponse getEnrolledStudents(String courseCode) {
        CourseEntity course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new NotFoundException("Course not found"));

        List<EnrollmentEntity> directEnrollments = enrollmentRepository.findAllByCourseCode(courseCode);

        Set<UserEntity> classStudents = new HashSet<>();
        if (course.getAssignedClasses() != null) {
            for (ClassEntity clazz : course.getAssignedClasses()) {
                List<UserEntity> usersInClass = userRepository.findAll().stream()
                        .filter(u -> u.getClazz() != null && u.getClazz().getId().equals(clazz.getId()))
                        .toList();
                classStudents.addAll(usersInClass);
            }
        }

        List<EnrollmentEntity> allEnrollments = new ArrayList<>(directEnrollments);

        Set<Long> enrolledUserIds = directEnrollments.stream()
                .map(e -> e.getUser().getId())
                .collect(Collectors.toSet());

        for (UserEntity student : classStudents) {
            if (!enrolledUserIds.contains(student.getId())) {
                EnrollmentEntity virtualEnrollment = new EnrollmentEntity();
                virtualEnrollment.setUser(student);
                virtualEnrollment.setCourse(course);
                virtualEnrollment.setStatus("ACTIVE");
                allEnrollments.add(virtualEnrollment);
            }
        }

        return mapper.toDto(course, allEnrollments);
    }
}