package moodlev2.academic.application.course;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.CourseCatalogApi;
import moodlev2.academic.api.CourseSummary;
import moodlev2.academic.api.ModuleSummary;
import moodlev2.academic.infrastructure.persistence.CourseModuleRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCatalogService implements CourseCatalogApi {

    private final CourseRepository courses;
    private final CourseModuleRepository modules;

    @Override
    public Optional<CourseSummary> findCourse(long id) {
        return courses.findById(id).map(CourseCatalogService::toSummary);
    }

    @Override
    public Optional<CourseSummary> findCourseByCode(String code) {
        return code == null
                ? Optional.empty()
                : courses.findByCode(code).map(CourseCatalogService::toSummary);
    }

    @Override
    public List<CourseSummary> findCourses(Collection<Long> ids) {
        return ids.isEmpty()
                ? List.of()
                : courses.findAllById(ids).stream().map(CourseCatalogService::toSummary).toList();
    }

    @Override
    public Optional<ModuleSummary> findModule(long moduleId) {
        return modules.findById(moduleId)
                .map(m -> new ModuleSummary(m.getId(), m.getCourse().getId(), m.getTitle()));
    }

    static CourseSummary toSummary(CourseEntity c) {
        return new CourseSummary(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getTerm(),
                c.getStatus(),
                c.getTeacherId(),
                c.getImageUrl());
    }
}
