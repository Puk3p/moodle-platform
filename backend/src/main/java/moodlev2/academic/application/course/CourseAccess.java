package moodlev2.academic.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.identity.domain.user.Role;
import moodlev2.identity.infrastructure.persistence.entity.UserEntity;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * Who may see or act on a course.
 *
 * <p>Every teacher (and admin) manages every course, so staff are never restricted here. A student
 * belongs to a course when enrolled directly or when their class is assigned to it. Anything else
 * is reported as "not found" rather than "forbidden", so course, quiz and assignment ids cannot be
 * probed to learn what exists.
 */
@Component
@RequiredArgsConstructor
public class CourseAccess {

    private final CourseRepository courseRepository;

    public static boolean isStaff(UserEntity user) {
        return user != null
                && user.getRoles() != null
                && (user.getRoles().contains(Role.TEACHER) || user.getRoles().contains(Role.ADMIN));
    }

    public boolean isMember(CourseEntity course, UserEntity user) {
        if (course == null || user == null) {
            return false;
        }
        if (isStaff(user)) {
            return true;
        }
        return course.getId() != null
                && user.getId() != null
                && courseRepository.isMember(course.getId(), user.getId());
    }

    /**
     * @throws NotFoundException when a student does not belong to the course
     */
    public void requireMember(CourseEntity course, UserEntity user) {
        if (!isMember(course, user)) {
            throw new NotFoundException("Course not found");
        }
    }
}
