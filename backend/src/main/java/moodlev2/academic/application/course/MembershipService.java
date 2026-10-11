package moodlev2.academic.application.course;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.MembershipApi;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.identity.api.UserSummary;
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
}
