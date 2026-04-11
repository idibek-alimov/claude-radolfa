package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.question.AskProductQuestionUseCase;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.domain.model.QuestionStatus;
import tj.radolfa.domain.model.User;

@Slf4j
@Service
public class AskProductQuestionService implements AskProductQuestionUseCase {

    private final LoadUserPort            loadUserPort;
    private final LoadProductBasePort     loadProductBasePort;
    private final SaveProductQuestionPort saveProductQuestionPort;

    public AskProductQuestionService(LoadUserPort loadUserPort,
                                     LoadProductBasePort loadProductBasePort,
                                     SaveProductQuestionPort saveProductQuestionPort) {
        this.loadUserPort            = loadUserPort;
        this.loadProductBasePort     = loadProductBasePort;
        this.saveProductQuestionPort = saveProductQuestionPort;
    }

    @Override
    @Transactional
    public Long execute(Long productBaseId, Long listingVariantId, Long authorId, String questionText) {
        // Resolve author name server-side — never trust the client
        User author = loadUserPort.loadById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + authorId));

        String authorName = (author.name() != null && !author.name().isBlank())
                ? author.name()
                : "Anonymous";

        // Validate product exists
        loadProductBasePort.findById(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: id=" + productBaseId));

        ProductQuestion question = new ProductQuestion(
                null,
                productBaseId,
                listingVariantId,
                authorId,
                authorName,
                questionText,
                null,
                null,
                QuestionStatus.PENDING,
                null
        );

        ProductQuestion saved = saveProductQuestionPort.save(question);
        log.info("[Q&A] Question submitted id={} productBaseId={} authorId={}",
                saved.getId(), productBaseId, authorId);

        return saved.getId();
    }
}
