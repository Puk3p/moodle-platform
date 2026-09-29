package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByCode(String code);

    List<CourseEntity> findAllByTeacherId(Long teacherId);

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

    /**
     * Whether the user belongs to the course: enrolled directly, or in a class the course is
     * assigned to. Same rule as {@link #findAllCoursesForStudent}, answered for one course.
     */
    @Query(
            "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CourseEntity c "
                    + "WHERE c.id = :courseId AND ("
                    + "EXISTS (SELECT e.id FROM EnrollmentEntity e "
                    + "WHERE e.course.id = c.id AND e.user.id = :userId) "
                    + "OR EXISTS (SELECT u.id FROM UserEntity u JOIN u.clazz cl "
                    + "WHERE u.id = :userId AND cl MEMBER OF c.assignedClasses))")
    boolean isMember(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
