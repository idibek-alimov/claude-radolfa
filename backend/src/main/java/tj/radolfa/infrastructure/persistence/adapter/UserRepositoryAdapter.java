package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.persistence.mappers.UserMapper;
import tj.radolfa.infrastructure.persistence.repository.UserRepository;

import java.util.Optional;

/**
 * Hexagonal adapter that bridges {@link LoadUserPort}
 * to the Spring Data {@link UserRepository}.
 */
@Component
public class UserRepositoryAdapter implements LoadUserPort {

    private final UserRepository repository;
    private final UserMapper     mapper;

    public UserRepositoryAdapter(UserRepository repository,
                                 UserMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    @Override
    public Optional<User> loadByPhone(String phone) {
        return repository.findByPhone(phone)
                .map(mapper::toUser);
    }
}
