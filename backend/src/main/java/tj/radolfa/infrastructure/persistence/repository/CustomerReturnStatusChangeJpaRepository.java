package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnStatusChangeEntity;

public interface CustomerReturnStatusChangeJpaRepository
        extends JpaRepository<CustomerReturnStatusChangeEntity, Long> {

    Page<CustomerReturnStatusChangeEntity> findByReturnId(Long returnId, Pageable pageable);
}
