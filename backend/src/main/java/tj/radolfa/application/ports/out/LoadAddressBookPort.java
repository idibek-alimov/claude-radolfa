package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.AddressBook;

import java.util.Optional;

public interface LoadAddressBookPort {
    Optional<AddressBook> load(Long userId);
}
