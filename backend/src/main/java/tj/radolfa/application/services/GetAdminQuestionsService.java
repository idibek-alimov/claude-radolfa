package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.question.GetAdminQuestionsUseCase;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.domain.model.QuestionStatus;

import java.time.Instant;

@Service
public class GetAdminQuestionsService implements GetAdminQuestionsUseCase {

    private final LoadProductQuestionPort loadProductQuestionPort;

    public GetAdminQuestionsService(LoadProductQuestionPort loadProductQuestionPort) {
        this.loadProductQuestionPort = loadProductQuestionPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionAdminView> getQuestions(QuestionStatus status,
                                                 String search,
                                                 Instant dateFrom,
                                                 Instant dateTo,
                                                 int page,
                                                 int size,
                                                 String sortBy,
                                                 String sortDir) {
        return loadProductQuestionPort.findAdminQuestions(
                status, search, dateFrom, dateTo, page, size, sortBy, sortDir);
    }
}
