package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyToReviewRequestDto(

        @NotBlank
        @Size(max = 3000)
        String replyText
) {}
