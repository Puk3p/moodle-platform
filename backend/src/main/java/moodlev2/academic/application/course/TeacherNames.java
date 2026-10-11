package moodlev2.academic.application.course;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import org.springframework.stereotype.Component;

/** Display names of the teachers of courses, looked up in one call per list. */
@Component
@RequiredArgsConstructor
public class TeacherNames {

    private final DirectoryApi directory;

    /** Full name per teacher id, for the teachers of these courses. */
    public Map<Long, String> of(Collection<CourseEntity> courses) {
        var ids =
                courses.stream()
                        .map(CourseEntity::getTeacherId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        return directory.findUsers(ids).stream()
                .collect(Collectors.toMap(UserSummary::id, UserSummary::fullName));
    }

    /** The course's teacher's full name, or {@code fallback} when it has none. */
    public String of(CourseEntity course, String fallback) {
        if (course.getTeacherId() == null) {
            return fallback;
        }
        return directory
                .findUser(course.getTeacherId())
                .map(UserSummary::fullName)
                .orElse(fallback);
    }
}
