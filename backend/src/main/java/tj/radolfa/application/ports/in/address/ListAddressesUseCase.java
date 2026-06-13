package tj.radolfa.application.ports.in.address;

import tj.radolfa.domain.model.Address;

import java.util.List;

public interface ListAddressesUseCase {

    /** Returns all addresses owned by the given user. */
    List<Address> execute(Long userId);
}
