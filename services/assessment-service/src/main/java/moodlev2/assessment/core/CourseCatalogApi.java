package moodlev2.assessment.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Courses and their modules, owned by core. */
public interface CourseCatalogApi {

    Optional<CourseSummary> findCourse(long id);

    List<CourseSummary> findCourses(Collection<Long> ids);

    Optional<ModuleSummary> findModule(long moduleId);
}
