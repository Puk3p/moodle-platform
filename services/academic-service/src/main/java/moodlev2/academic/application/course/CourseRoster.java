package moodlev2.academic.application.course;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.directory.DirectoryApi;
import moodlev2.academic.directory.UserSummary;
import moodlev2.academic.infrastructure.persistence.EnrollmentRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.EnrollmentEntity;
import org.springframework.stereotype.Component;

/**
 * The people in a course: everyone enrolled directly, plus everyone placed in a class the course is
 * assigned to. The one place this rule is spelled out for listing purposes ({@link CourseAccess}
 * answers it for a single user).
 */
@Component
@RequiredArgsConstructor
public class CourseRoster {

    private final EnrollmentRepository enrollmentRepository;
    private final DirectoryApi directory;

    /** Direct enrollments first (in enrollment order), then class members by id; no duplicates. */
    public List<UserSummary> studentsOf(CourseEntity course) {
        List<Long> enrolledIds =
                enrollmentRepository.findAllByCourseCode(course.getCode()).stream()
                        .map(EnrollmentEntity::getUserId)
                        .toList();
        Map<Long, UserSummary> byId =
                directory.findUsers(enrolledIds).stream()
                        .collect(Collectors.toMap(UserSummary::id, Function.identity()));

        Map<Long, UserSummary> roster = new LinkedHashMap<>();
        for (Long id : enrolledIds) {
            UserSummary user = byId.get(id);
            if (user != null) {
                roster.putIfAbsent(id, user);
            }
        }
        List<UserSummary> classMembers =
                new ArrayList<>(directory.findUsersInClasses(course.getClassIds()));
        classMembers.sort(Comparator.comparingLong(UserSummary::id));
        for (UserSummary member : classMembers) {
            roster.putIfAbsent(member.id(), member);
        }
        return new ArrayList<>(roster.values());
    }
}
