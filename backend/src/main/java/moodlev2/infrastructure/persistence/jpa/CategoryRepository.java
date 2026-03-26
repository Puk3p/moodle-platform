package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findByParentIsNullOrderBySortOrderAsc();
}
