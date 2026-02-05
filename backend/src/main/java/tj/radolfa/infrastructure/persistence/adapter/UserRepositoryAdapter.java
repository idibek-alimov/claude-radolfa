package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;
import tj.radolfa.infrastructure.persistence.mappers.UserMapper;
import tj.radolfa.infrastructure.persistence.repository.UserRepository;

import java.util.Optional;

/**
 * Hexagonal adapter that bridges {@link LoadUserPort} and {@link SaveUserPort}
 * to the Spring Data {@link UserRepository}.
 */
@Component
public class UserRepositoryAdapter implements LoadUserPort, SaveUserPort {

    private final UserRepository repository;
    private final UserMapper mapper;

    public UserRepositoryAdapter(UserRepository repository,
            UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> loadByPhone(String phone) {
        return repository.findByPhone(phone)
                .map(mapper::toUser);
    }

    @Override
    public Optional<User> loadById(Long id) {
        return repository.findById(id)
                .map(mapper::toUser);
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = repository.save(entity);
        return mapper.toUser(saved);
    }
}
