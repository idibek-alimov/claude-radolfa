package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.question.AnswerProductQuestionUseCase;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductQuestion;

@Slf4j
@Service
public class AnswerProductQuestionService implements AnswerProductQuestionUseCase {

    private final LoadProductQuestionPort loadProductQuestionPort;
    private final SaveProductQuestionPort saveProductQuestionPort;

    public AnswerProductQuestionService(LoadProductQuestionPort loadProductQuestionPort,
                                        SaveProductQuestionPort saveProductQuestionPort) {
        this.loadProductQuestionPort = loadProductQuestionPort;
        this.saveProductQuestionPort = saveProductQuestionPort;
    }

    @Override
    @Transactional
    public void execute(Long questionId, String answerText) {
        ProductQuestion question = loadProductQuestionPort.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: id=" + questionId));

        question.publish(answerText);
        saveProductQuestionPort.save(question);
        log.info("[Q&A] Question answered and published id={}", questionId);
    }
}
