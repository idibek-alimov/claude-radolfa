package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.HomeBannerEntity;

import java.util.List;

public interface HomeBannerRepository extends JpaRepository<HomeBannerEntity, Long> {

    List<HomeBannerEntity> findByActiveTrue();
}
