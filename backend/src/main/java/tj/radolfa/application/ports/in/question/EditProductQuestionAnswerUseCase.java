package tj.radolfa.application.ports.in.question;

public interface EditProductQuestionAnswerUseCase {

    void execute(Long questionId, String newAnswerText);
}
