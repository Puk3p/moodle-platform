package moodlev2.assessment.infrastructure.persistence;

import java.util.List;
import moodlev2.assessment.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findByParentIsNullOrderBySortOrderAsc();
}
