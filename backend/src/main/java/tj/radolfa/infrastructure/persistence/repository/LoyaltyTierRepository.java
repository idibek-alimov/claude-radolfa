package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity;

import java.util.List;
import java.util.Optional;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTierEntity, Long> {

    Optional<LoyaltyTierEntity> findByName(String name);

    List<LoyaltyTierEntity> findAllByOrderByDisplayOrderAsc();
}
