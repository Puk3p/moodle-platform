package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModuleItemRepository extends JpaRepository<ModuleItemEntity, Long> {
    @Query(
            "SELECT mi FROM ModuleItemEntity mi "
                    + "JOIN mi.module m JOIN m.course c JOIN EnrollmentEntity e ON c.id = e.course.id "
                    + "WHERE e.user.email = :email AND (mi.type = 'resource' OR mi.fileType IS NOT NULL)")
    List<ModuleItemEntity> findResourcesByUserEmail(String email);

    @Query(
            """
        SELECT mi FROM ModuleItemEntity mi
        JOIN mi.module m
        JOIN m.course c
        WHERE c.code = :courseCode
        AND (mi.type = 'resource' OR mi.fileType IS NOT NULL)
        ORDER BY mi.createdAt DESC
    """)
    List<ModuleItemEntity> findAllResourcesByCourseCode(String courseCode);

    List<ModuleItemEntity> findTop5ByOrderByCreatedAtDesc();
}
