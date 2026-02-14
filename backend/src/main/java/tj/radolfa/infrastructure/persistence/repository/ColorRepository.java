package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<ColorEntity, Long> {

    Optional<ColorEntity> findByColorKey(String colorKey);
}
