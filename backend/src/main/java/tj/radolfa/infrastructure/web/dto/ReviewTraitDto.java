package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.ReviewTraitInputType;

public record ReviewTraitDto(
        Long id,
        String key,
        String labelI18n,
        ReviewTraitInputType inputType
) {}
