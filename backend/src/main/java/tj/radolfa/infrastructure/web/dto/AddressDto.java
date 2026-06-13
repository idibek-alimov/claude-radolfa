package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressLabel;

public record AddressDto(
        Long id,
        AddressLabel label,
        String recipientName,
        String phone,
        String line1,
        String city,
        String postalCode,
        String country,
        boolean isDefault) {

    public static AddressDto from(Address address) {
        return new AddressDto(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getLine1(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefault()
        );
    }
}
