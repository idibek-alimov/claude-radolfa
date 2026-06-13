package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Address;

import java.util.List;
import java.util.Optional;

public interface LoadAddressPort {

    /** All addresses owned by the given user. */
    List<Address> findByUserId(Long userId);

    /** Owner-scoped lookup — returns empty if the address does not exist or is not owned by {@code userId}. */
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
