package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTierRequestDto(@NotNull Long tierId) {}
