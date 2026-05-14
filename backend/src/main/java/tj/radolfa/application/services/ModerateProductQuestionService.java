package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.question.ModerateProductQuestionUseCase;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductQuestion;

@Slf4j
@Service
public class ModerateProductQuestionService implements ModerateProductQuestionUseCase {

    private final LoadProductQuestionPort loadProductQuestionPort;
    private final SaveProductQuestionPort saveProductQuestionPort;

    public ModerateProductQuestionService(LoadProductQuestionPort loadProductQuestionPort,
                                          SaveProductQuestionPort saveProductQuestionPort) {
        this.loadProductQuestionPort = loadProductQuestionPort;
        this.saveProductQuestionPort = saveProductQuestionPort;
    }

    @Override
    @Transactional
    public void reject(Long questionId) {
        ProductQuestion question = loadProductQuestionPort.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: id=" + questionId));

        question.reject();
        saveProductQuestionPort.save(question);
        log.info("[Q&A] Question rejected id={}", questionId);
    }
}
