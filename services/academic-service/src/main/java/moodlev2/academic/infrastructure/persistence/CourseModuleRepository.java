package moodlev2.academic.infrastructure.persistence;

import moodlev2.academic.infrastructure.persistence.entity.CourseModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModuleEntity, Long> {}
