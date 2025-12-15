package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ModuleItemRepository extends JpaRepository<ModuleItemEntity, Long> {
    @Query("SELECT mi FROM ModuleItemEntity mi " +
            "JOIN mi.module m JOIN m.course c JOIN EnrollmentEntity e ON c.id = e.course.id " +
            "WHERE e.user.email = :email AND (mi.type = 'resource' OR mi.fileType IS NOT NULL)")
    List<ModuleItemEntity> findResourcesByUserEmail(String email);
}