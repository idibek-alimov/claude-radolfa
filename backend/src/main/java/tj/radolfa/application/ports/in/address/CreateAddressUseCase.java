package tj.radolfa.application.ports.in.address;

import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressLabel;

public interface CreateAddressUseCase {

    Address execute(Command command);

    record Command(Long userId,
                    AddressLabel label,
                    String recipientName,
                    String phone,
                    String line1,
                    String city,
                    String postalCode,
                    String country,
                    boolean isDefault) {}
}
