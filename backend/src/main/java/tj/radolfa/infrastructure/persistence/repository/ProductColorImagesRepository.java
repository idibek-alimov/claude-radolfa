package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ProductColorImagesEntity;

import java.util.Optional;

public interface ProductColorImagesRepository extends JpaRepository<ProductColorImagesEntity, Long> {

    @Query("""
            SELECT e FROM ProductColorImagesEntity e
            WHERE e.template.id = :templateId
              AND (e.colorKey = :colorKey OR (e.colorKey IS NULL AND :colorKey IS NULL))
            """)
    Optional<ProductColorImagesEntity> findByTemplateIdAndColorKey(
            @Param("templateId") Long templateId,
            @Param("colorKey") String colorKey);
}
