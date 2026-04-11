package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.domain.model.QuestionStatus;
import tj.radolfa.infrastructure.persistence.entity.ProductQuestionEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductQuestionMapper;
import tj.radolfa.infrastructure.persistence.repository.ProductQuestionRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ProductQuestionAdapter implements LoadProductQuestionPort, SaveProductQuestionPort {

    private final ProductQuestionRepository repository;
    private final ProductQuestionMapper mapper;

    public ProductQuestionAdapter(ProductQuestionRepository repository, ProductQuestionMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    // ---- LoadProductQuestionPort ---------------------------------------

    @Override
    public Optional<ProductQuestion> findById(Long id) {
        return repository.findById(id).map(mapper::toProductQuestion);
    }

    @Override
    public Page<ProductQuestion> findPublishedByProductBase(Long productBaseId, Pageable pageable) {
        return repository
                .findByProductBaseIdAndStatus(productBaseId, QuestionStatus.PUBLISHED, pageable)
                .map(mapper::toProductQuestion);
    }

    @Override
    public List<ProductQuestion> findPendingOldestFirst(int limit) {
        return repository
                .findByStatusOrderByCreatedAtAsc(QuestionStatus.PENDING, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(mapper::toProductQuestion)
                .toList();
    }

    @Override
    public List<QuestionAdminView> findPendingWithContextOldestFirst(int limit) {
        // SQL column order: id, author_name, question_text, answer_text, answered_at, created_at,
        //                   product_base_id(6), product_name(7), listing_variant_id(8),
        //                   product_slug(9), thumbnail_url(10), color_name(11), color_hex(12)
        // Record constructor order: id, authorName, questionText, answerText, answeredAt, createdAt,
        //                           productBaseId, productName, productSlug, thumbnailUrl,
        //                           listingVariantId, colorName, colorHex
        List<Object[]> rows = repository.findPendingWithContext(limit);
        return rows.stream()
                .map(row -> new QuestionAdminView(
                        ((Number) row[0]).longValue(),                                         // id
                        (String)  row[1],                                                      // authorName
                        (String)  row[2],                                                      // questionText
                        (String)  row[3],                                                      // answerText
                        toInstant(row[4]),                                                     // answeredAt
                        toInstant(row[5]),                                                     // createdAt
                        ((Number) row[6]).longValue(),                                         // productBaseId
                        (String)  row[7],                                                      // productName
                        (String)  row[9],                                                      // productSlug (SQL col 9)
                        (String)  row[10],                                                     // thumbnailUrl (SQL col 10)
                        row[8] != null ? Long.valueOf(((Number) row[8]).longValue()) : null,   // listingVariantId (SQL col 8)
                        (String)  row[11],                                                     // colorName
                        (String)  row[12]                                                      // colorHex
                ))
                .toList();
    }

    private static Instant toInstant(Object val) {
        if (val == null) return null;
        if (val instanceof Instant inst) return inst;
        if (val instanceof Timestamp ts) return ts.toInstant();
        if (val instanceof OffsetDateTime odt) return odt.toInstant();
        throw new IllegalStateException("Unexpected timestamp type: " + val.getClass());
    }

    // ---- SaveProductQuestionPort ---------------------------------------

    @Override
    public ProductQuestion save(ProductQuestion question) {
        ProductQuestionEntity entity;

        if (question.getId() != null) {
            entity = repository.findById(question.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ProductQuestion not found: " + question.getId()));
            // Update only mutable moderation fields — immutable fields (authorName,
            // questionText, createdAt) are preserved from the loaded entity.
            entity.setStatus(question.getStatus());
            entity.setAnswerText(question.getAnswerText());
            entity.setAnsweredAt(question.getAnsweredAt());
        } else {
            entity = mapper.toEntity(question);
        }

        return mapper.toProductQuestion(repository.save(entity));
    }
}
