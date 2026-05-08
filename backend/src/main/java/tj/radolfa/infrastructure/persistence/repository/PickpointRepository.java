package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.PickpointEntity;

import java.util.List;

public interface PickpointRepository extends JpaRepository<PickpointEntity, Long> {
    List<PickpointEntity> findAllByActiveTrue();

    List<PickpointEntity> findAllByOrderByActiveDescNameAsc();

    @Query("SELECT p FROM PickpointEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(p.address) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY p.active DESC, p.name ASC")
    List<PickpointEntity> searchAll(@Param("q") String q);
}
