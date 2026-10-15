package moodlev2.academic.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only lookup of courses and their modules. Lists come back in no particular order and
 * silently skip ids that do not exist.
 */
public interface CourseCatalogApi {

    Optional<CourseSummary> findCourse(long id);

    Optional<CourseSummary> findCourseByCode(String code);

    List<CourseSummary> findCourses(Collection<Long> ids);

    Optional<ModuleSummary> findModule(long moduleId);
}
