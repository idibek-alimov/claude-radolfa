package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.domain.model.QuestionStatus;
import tj.radolfa.infrastructure.persistence.entity.ProductQuestionEntity;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestionEntity, Long> {

    Page<ProductQuestionEntity> findByProductBaseIdAndStatus(Long productBaseId, QuestionStatus status, Pageable pageable);

}
