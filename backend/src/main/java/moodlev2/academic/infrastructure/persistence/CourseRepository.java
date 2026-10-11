package moodlev2.academic.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByCode(String code);

    List<CourseEntity> findAllByTeacherId(Long teacherId);

    /** Courses the user is enrolled in directly (not through a class). */
    @Query(
            "SELECT c FROM CourseEntity c JOIN EnrollmentEntity e ON c.id = e.course.id "
                    + "WHERE e.userId = :userId")
    List<CourseEntity> findAllByEnrolledUserId(@Param("userId") Long userId);

    /**
     * Courses a student belongs to: enrolled directly, or through their class ({@code classId},
     * {@code null} when they have none) being assigned to the course.
     */
    @Query(
            "SELECT c FROM CourseEntity c "
                    + "WHERE EXISTS (SELECT e.id FROM EnrollmentEntity e "
                    + "WHERE e.course = c AND e.userId = :userId) "
                    + "OR (:classId IS NOT NULL AND :classId MEMBER OF c.classIds)")
    List<CourseEntity> findAllCoursesForStudent(
            @Param("userId") Long userId, @Param("classId") Long classId);

    /**
     * Whether the student belongs to the course; same rule as {@link #findAllCoursesForStudent}.
     */
    @Query(
            "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CourseEntity c "
                    + "WHERE c.id = :courseId AND ("
                    + "EXISTS (SELECT e.id FROM EnrollmentEntity e "
                    + "WHERE e.course = c AND e.userId = :userId) "
                    + "OR (:classId IS NOT NULL AND :classId MEMBER OF c.classIds))")
    boolean isMember(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId,
            @Param("classId") Long classId);
}
