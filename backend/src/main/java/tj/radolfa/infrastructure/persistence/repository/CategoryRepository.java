package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByName(String name);

    Optional<CategoryEntity> findBySlug(String slug);

    List<CategoryEntity> findByParentId(Long parentId);

    List<CategoryEntity> findByParentIsNull();

    @Query("SELECT COUNT(pb) > 0 FROM ProductBaseEntity pb WHERE pb.category.id = :categoryId")
    boolean existsProductBasesByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = """
        WITH RECURSIVE descendants AS (
            SELECT id FROM categories WHERE id = :rootId
            UNION ALL
            SELECT c.id FROM categories c JOIN descendants d ON c.parent_id = d.id
        )
        SELECT id FROM descendants
        """, nativeQuery = true)
    List<Long> findAllDescendantIds(@Param("rootId") Long rootId);
}
