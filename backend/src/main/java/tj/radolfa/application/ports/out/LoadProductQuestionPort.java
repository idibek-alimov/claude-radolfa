package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.Optional;

public interface LoadProductQuestionPort {

    Optional<ProductQuestion> findById(Long id);

    Page<ProductQuestion> findPublishedByProductBase(Long productBaseId, Pageable pageable);

    /**
     * Returns a paginated, filtered, sorted page of questions for the admin queue.
     * {@code page} is 1-based. {@code search} and date range are optional (null = no filter).
     * {@code sortBy} is "createdAt" or "answeredAt"; {@code sortDir} is "ASC" or "DESC".
     */
    Page<QuestionAdminView> findAdminQuestions(QuestionStatus status,
                                               String search,
                                               Instant dateFrom,
                                               Instant dateTo,
                                               int page,
                                               int size,
                                               String sortBy,
                                               String sortDir);
}
