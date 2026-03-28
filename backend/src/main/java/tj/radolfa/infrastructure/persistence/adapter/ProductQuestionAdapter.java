package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.domain.model.QuestionStatus;
import tj.radolfa.infrastructure.persistence.mappers.ProductQuestionMapper;
import tj.radolfa.infrastructure.persistence.repository.ProductQuestionRepository;

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
                .findByProductBaseIdAndStatus(productBaseId, QuestionStatus.PUBLISHED.name(), pageable)
                .map(mapper::toProductQuestion);
    }

    @Override
    public List<ProductQuestion> findPendingOldestFirst(int limit) {
        return repository
                .findByStatusOrderByCreatedAtAsc(QuestionStatus.PENDING.name(), PageRequest.of(0, limit))
                .stream()
                .map(mapper::toProductQuestion)
                .toList();
    }

    // ---- SaveProductQuestionPort ---------------------------------------

    @Override
    public ProductQuestion save(ProductQuestion question) {
        return mapper.toProductQuestion(repository.save(mapper.toEntity(question)));
    }
}
