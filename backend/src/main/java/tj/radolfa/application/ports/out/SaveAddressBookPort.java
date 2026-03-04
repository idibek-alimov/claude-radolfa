package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.AddressBook;

public interface SaveAddressBookPort {
    AddressBook save(AddressBook book);
}
