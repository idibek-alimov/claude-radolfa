package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeUserRoleRequestDto(@NotBlank String role) {}
