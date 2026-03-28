package tj.radolfa.application.ports.in.question;

public interface AskProductQuestionUseCase {

    Long execute(Long productBaseId, Long authorId, String questionText);
}
