package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByName(String name);

    Optional<CategoryEntity> findBySlug(String slug);

    List<CategoryEntity> findByParentId(Long parentId);

    List<CategoryEntity> findByParentIsNull();
}
