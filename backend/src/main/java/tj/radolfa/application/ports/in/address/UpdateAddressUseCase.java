package tj.radolfa.application.ports.in.address;

import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressLabel;

public interface UpdateAddressUseCase {

    Address execute(Command command);

    /** {@code isDefault} is intentionally absent — the default flag is managed only via {@link SetDefaultAddressUseCase}. */
    record Command(Long id,
                    Long userId,
                    AddressLabel label,
                    String recipientName,
                    String phone,
                    String line1,
                    String city,
                    String postalCode,
                    String country) {}
}
