package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressBook;
import tj.radolfa.infrastructure.persistence.entity.AddressBookEntity;
import tj.radolfa.infrastructure.persistence.entity.AddressEntity;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressBookMapper {

    /**
     * Maps AddressBookEntity to the AddressBook domain object.
     * Uses a default method because AddressBook is a mutable class (not a record).
     */
    default AddressBook toAddressBook(AddressBookEntity entity) {
        if (entity == null) return null;
        List<Address> addresses = entity.getAddresses() != null
                ? entity.getAddresses().stream().map(this::toAddress).toList()
                : new ArrayList<>();
        return new AddressBook(entity.getUserId(), addresses);
    }

    /**
     * Maps AddressEntity to the Address domain object.
     */
    default Address toAddress(AddressEntity entity) {
        if (entity == null) return null;
        return new Address(
                entity.getId(),
                entity.getLabel(),
                entity.getRecipientName(),
                entity.getPhone(),
                entity.getStreet(),
                entity.getCity(),
                entity.getRegion(),
                entity.getCountry(),
                entity.isDefault());
    }
}
