package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.AddressBook;

public interface RemoveAddressUseCase {
    AddressBook execute(Long userId, Long addressId);
}
