package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AskQuestionRequestDto(

        @NotNull
        Long productBaseId,

        @NotBlank
        @Size(max = 2000)
        String questionText
) {}
