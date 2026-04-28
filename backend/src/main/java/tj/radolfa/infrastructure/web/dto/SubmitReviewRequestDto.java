package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tj.radolfa.domain.model.MatchingSize;

import java.util.List;

public record SubmitReviewRequestDto(

        @NotNull
        Long listingVariantId,

        Long skuId,

        @NotNull
        Long orderId,

        @Min(1) @Max(5)
        int rating,

        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 5000)
        String body,

        @Size(max = 1000)
        String pros,

        @Size(max = 1000)
        String cons,

        MatchingSize matchingSize,

        @Size(max = 5)
        List<String> photoUrls
) {}
