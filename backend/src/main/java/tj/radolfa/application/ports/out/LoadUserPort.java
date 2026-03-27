package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Out-Port: load a user by phone number.
 */
public interface LoadUserPort {
    Optional<User> loadByPhone(String phone);

    Optional<User> loadById(Long id);

    /** Returns all users whose loyalty tier is NOT permanently locked. */
    List<User> findAllNonPermanent();
}
