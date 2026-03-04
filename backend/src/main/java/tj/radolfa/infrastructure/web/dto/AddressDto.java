package tj.radolfa.infrastructure.web.dto;

public record AddressDto(
        Long id,
        String label,
        String recipientName,
        String phone,
        String street,
        String city,
        String region,
        String country,
        boolean isDefault
) {}
