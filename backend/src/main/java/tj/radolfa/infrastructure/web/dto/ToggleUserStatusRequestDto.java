package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleUserStatusRequestDto(@NotNull Boolean enabled) {}
