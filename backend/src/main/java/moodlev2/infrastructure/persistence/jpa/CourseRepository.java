package moodlev2.infrastructure.persistence.jpa;


import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByCode(String code);

    @Query("SELECT c FROM CourseEntity c JOIN EnrollmentEntity e ON c.id = e.course.id WHERE e.user.email = :email")
    List<CourseEntity> findAllByUserEmail(String email);
}