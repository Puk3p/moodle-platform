package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByCode(String code);

    @Query(
            "SELECT c FROM CourseEntity c JOIN EnrollmentEntity e ON c.id = e.course.id WHERE e.user.email = :email")
    List<CourseEntity> findAllByUserEmail(String email);

    @Query(
            "SELECT DISTINCT c FROM CourseEntity c "
                    + "LEFT JOIN c.enrollments e "
                    + "LEFT JOIN c.assignedClasses cl "
                    + "WHERE e.user.id = :userId "
                    + "OR cl.id = (SELECT u.clazz.id FROM UserEntity u WHERE u.id = :userId)")
    List<CourseEntity> findAllCoursesForStudent(@Param("userId") Long userId);
}
