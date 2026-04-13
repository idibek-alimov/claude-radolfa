package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerQuestionRequestDto(

        @NotBlank
        @Size(max = 2000)
        String answerText
) {}
