package tj.radolfa.application.ports.in.question;

public interface AnswerProductQuestionUseCase {

    void execute(Long questionId, String answerText);
}
