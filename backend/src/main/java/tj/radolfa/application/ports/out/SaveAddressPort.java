package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Address;

public interface SaveAddressPort {

    Address save(Address address);

    /** Owner-scoped delete — no-op if {@code id} is not owned by {@code userId}. */
    void delete(Long id, Long userId);

    /** Clears the default flag on every address owned by {@code userId}. */
    void clearDefaultForUser(Long userId);
}
