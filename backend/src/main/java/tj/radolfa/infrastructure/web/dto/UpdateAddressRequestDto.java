package tj.radolfa.infrastructure.web.dto;

/**
 * All fields are nullable to support partial updates. The controller
 * falls back to the current persisted value when a field is null.
 */
public record UpdateAddressRequestDto(
        String label,
        String recipientName,
        String phone,
        String street,
        String city,
        String region,
        String country,
        Boolean isDefault
) {}
