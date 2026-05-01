package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.ReviewTraitInputType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RatingSummaryResponseDto(
        BigDecimal averageRating,
        int reviewCount,
        Map<Integer, Integer> distribution,
        int sizeAccurate,
        int sizeRunsSmall,
        int sizeRunsLarge,
        List<TraitAggregateDto> traitAggregates
) {
    public record TraitAggregateDto(
            String traitKey,
            String labelI18n,
            ReviewTraitInputType inputType,
            BigDecimal average,
            int count
    ) {}

    public static RatingSummaryResponseDto empty() {
        return new RatingSummaryResponseDto(
                BigDecimal.ZERO, 0,
                Map.of(1, 0, 2, 0, 3, 0, 4, 0, 5, 0),
                0, 0, 0,
                List.of());
    }
}
