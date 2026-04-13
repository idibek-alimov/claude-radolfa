package tj.radolfa.application.ports.in.question;

import org.springframework.data.domain.Page;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.domain.model.QuestionStatus;

import java.time.Instant;

public interface GetAdminQuestionsUseCase {

    Page<QuestionAdminView> getQuestions(QuestionStatus status,
                                         String search,
                                         Instant dateFrom,
                                         Instant dateTo,
                                         int page,
                                         int size,
                                         String sortBy,
                                         String sortDir);
}
