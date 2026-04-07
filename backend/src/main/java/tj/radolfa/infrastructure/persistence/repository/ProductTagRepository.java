package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ProductTagEntity;

import java.util.Optional;

public interface ProductTagRepository extends JpaRepository<ProductTagEntity, Long> {
    Optional<ProductTagEntity> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT COUNT(lv) FROM ListingVariantEntity lv JOIN lv.tags t WHERE t.id = :tagId")
    long countVariantsUsingTag(@Param("tagId") Long tagId);
}
