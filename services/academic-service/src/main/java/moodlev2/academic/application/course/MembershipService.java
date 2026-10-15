package moodlev2.academic.application.course;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.CourseSummary;
import moodlev2.academic.api.MembershipApi;
import moodlev2.academic.directory.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService implements MembershipApi {

    private final CourseRepository courseRepository;
    private final CourseAccess courseAccess;
    private final CourseRoster roster;

    @Override
    public boolean isMember(long courseId, UserSummary user) {
        return courseRepository
                .findById(courseId)
                .map(course -> courseAccess.isMember(course, user))
                .orElse(false);
    }

    @Override
    public List<UserSummary> studentsOf(long courseId) {
        return courseRepository.findById(courseId).map(roster::studentsOf).orElse(List.of());
    }

    @Override
    public List<CourseSummary> coursesOfStudent(UserSummary user) {
        return courseRepository.findAllCoursesForStudent(user.id(), user.classId()).stream()
                .map(CourseCatalogService::toSummary)
                .toList();
    }

    @Override
    public List<CourseSummary> coursesTaughtBy(long teacherId) {
        return courseRepository.findAllByTeacherId(teacherId).stream()
                .map(CourseCatalogService::toSummary)
                .toList();
    }

    @Override
    public List<CourseSummary> coursesInvolving(UserSummary user) {
        Map<Long, CourseSummary> all = new LinkedHashMap<>();
        coursesTaughtBy(user.id()).forEach(c -> all.put(c.id(), c));
        coursesOfStudent(user).forEach(c -> all.putIfAbsent(c.id(), c));
        return List.copyOf(all.values());
    }
}
