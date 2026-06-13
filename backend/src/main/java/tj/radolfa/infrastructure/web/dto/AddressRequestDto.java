package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tj.radolfa.domain.model.AddressLabel;

/**
 * Request body for creating/updating an address. The owner id and {@code isDefault}
 * (on update) are never accepted here — ownership comes from the security principal,
 * and the default flag is managed exclusively via {@code PATCH /addresses/{id}/default}.
 */
public record AddressRequestDto(
        @NotNull AddressLabel label,
        @NotBlank String recipientName,
        @NotBlank String phone,
        @NotBlank String line1,
        @NotBlank String city,
        String postalCode,
        @NotBlank String country,
        boolean isDefault) {
}
