package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.question.GetPendingQuestionsUseCase;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionAdminView;

import java.util.List;

@Service
public class GetPendingQuestionsService implements GetPendingQuestionsUseCase {

    private final LoadProductQuestionPort loadProductQuestionPort;

    public GetPendingQuestionsService(LoadProductQuestionPort loadProductQuestionPort) {
        this.loadProductQuestionPort = loadProductQuestionPort;
    }

    @Override
    public List<QuestionAdminView> getPending(int limit) {
        return loadProductQuestionPort.findPendingWithContextOldestFirst(limit);
    }
}
