package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddAddressRequestDto(
        @NotBlank String label,
        @NotBlank String recipientName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-()]{7,20}$", message = "Invalid phone number") String phone,
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String region,
        String country,         // nullable — defaults to "Tajikistan" in domain
        boolean isDefault
) {}
