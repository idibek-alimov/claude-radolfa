package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.AddressBook;

public interface AddAddressUseCase {
    AddressBook execute(Long userId,
                        String label,
                        String recipientName,
                        String phone,
                        String street,
                        String city,
                        String region,
                        String country,
                        boolean isDefault);
}
