package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ProductQuestionEntity;

import java.util.List;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestionEntity, Long> {

    Page<ProductQuestionEntity> findByProductBaseIdAndStatus(Long productBaseId, String status, Pageable pageable);

    Page<ProductQuestionEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
