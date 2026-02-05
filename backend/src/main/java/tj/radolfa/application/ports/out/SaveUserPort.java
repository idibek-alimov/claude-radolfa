package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.User;

/**
 * Out-Port: save or update a user in the persistence layer.
 */
public interface SaveUserPort {

    /**
     * Saves a new user or updates an existing one.
     *
     * @param user the user to save
     * @return the saved user with generated ID (if new)
     */
    User save(User user);
}
