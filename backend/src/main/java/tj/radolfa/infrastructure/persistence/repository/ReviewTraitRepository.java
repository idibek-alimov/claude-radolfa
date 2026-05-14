package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ReviewTraitEntity;

import java.util.Optional;

public interface ReviewTraitRepository extends JpaRepository<ReviewTraitEntity, Long> {

    Optional<ReviewTraitEntity> findByTraitKey(String traitKey);

    boolean existsByTraitKey(String traitKey);
}
