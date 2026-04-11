package tj.radolfa.application.ports.in.question;

import tj.radolfa.application.readmodel.QuestionAdminView;

import java.util.List;

public interface GetPendingQuestionsUseCase {
    List<QuestionAdminView> getPending(int limit);
}
