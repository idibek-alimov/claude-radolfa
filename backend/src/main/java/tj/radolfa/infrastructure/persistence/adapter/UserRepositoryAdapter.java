package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.application.ports.out.SearchUsersPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;
import tj.radolfa.infrastructure.persistence.mappers.UserMapper;
import tj.radolfa.infrastructure.persistence.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Hexagonal adapter that bridges {@link LoadUserPort}, {@link SaveUserPort},
 * and {@link SearchUsersPort} to the Spring Data {@link UserRepository}.
 */
@Component
public class UserRepositoryAdapter implements LoadUserPort, SaveUserPort, SearchUsersPort {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final EntityManager em;

    public UserRepositoryAdapter(UserRepository repository,
            UserMapper mapper,
            EntityManager em) {
        this.repository = repository;
        this.mapper = mapper;
        this.em = em;
    }

    @Override
    public Optional<User> loadByPhone(String phone) {
        return repository.findByPhone(phone)
                .map(mapper::toUser);
    }

    @Override
    public Optional<User> loadById(Long id) {
        return repository.findByIdWithTier(id)
                .map(mapper::toUser);
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        // The mapper builds a detached LoyaltyTierEntity with id but null @Version,
        // which Hibernate 6 rejects on flush. Replace with managed proxies so the
        // session treats them as already-persistent references (no SELECT needed).
        entity.setTier(managedTierProxy(entity.getTier()));
        entity.setLowestTierEver(managedTierProxy(entity.getLowestTierEver()));
        UserEntity saved = repository.save(entity);
        // Reload with tier eagerly fetched to avoid LazyInitializationException
        return repository.findByIdWithTier(saved.getId())
                .map(mapper::toUser)
                .orElseThrow();
    }

    private LoyaltyTierEntity managedTierProxy(LoyaltyTierEntity tier) {
        if (tier == null || tier.getId() == null) return tier;
        return em.getReference(LoyaltyTierEntity.class, tier.getId());
    }

    @Override
    public List<User> findAllNonPermanent() {
        return repository.findAllNonPermanent().stream()
                .map(mapper::toUser)
                .toList();
    }

    @Override
    public PageResult<User> searchUsers(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserEntity> result = repository.searchUsers(query, pageable);

        return new PageResult<>(
                result.getContent().stream().map(mapper::toUser).toList(),
                result.getTotalElements(),
                page,
                size,
                !result.hasNext());
    }

    @Override
    public List<User> findByRoleAndEnabledTrue(UserRole role) {
        return repository.findByRoleAndEnabledTrue(role).stream()
                .map(mapper::toUser)
                .toList();
    }

    @Override
    public PageResult<User> searchUsersByRoles(String query, Collection<UserRole> roles, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserEntity> result = repository.searchUsersByRoles(query, roles, pageable);

        return new PageResult<>(
                result.getContent().stream().map(mapper::toUser).toList(),
                result.getTotalElements(),
                page,
                size,
                !result.hasNext());
    }
}
