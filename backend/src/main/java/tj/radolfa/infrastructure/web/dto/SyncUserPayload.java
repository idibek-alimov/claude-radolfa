package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import tj.radolfa.domain.model.UserRole;

public record SyncUserPayload(
        @NotBlank String phone,
        String name,
        String email,
        UserRole role,
        Boolean enabled,
        Integer loyaltyPoints) {
}
