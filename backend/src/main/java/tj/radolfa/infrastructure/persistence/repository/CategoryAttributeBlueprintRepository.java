package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.CategoryAttributeBlueprintEntity;

import java.util.List;

public interface CategoryAttributeBlueprintRepository
        extends JpaRepository<CategoryAttributeBlueprintEntity, Long> {

    List<CategoryAttributeBlueprintEntity> findByCategoryIdOrderBySortOrderAsc(Long categoryId);
}
