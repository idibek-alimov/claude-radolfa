package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnEntity;

import java.util.List;

public interface CustomerReturnJpaRepository extends JpaRepository<CustomerReturnEntity, Long> {
    List<CustomerReturnEntity> findAllByOrderId(Long orderId);
    Page<CustomerReturnEntity> findByPickpointIdAndStatus(Long pickpointId, CustomerReturnStatus status, Pageable pageable);
}
