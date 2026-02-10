package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;

import java.util.List;
import java.util.Optional;

public interface ListingVariantRepository extends JpaRepository<ListingVariantEntity, Long> {

    Optional<ListingVariantEntity> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey);

    Optional<ListingVariantEntity> findBySlug(String slug);

    List<ListingVariantEntity> findByProductBaseId(Long productBaseId);
}
