package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.SaveLoyaltyTierPort;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity;
import tj.radolfa.infrastructure.persistence.mappers.LoyaltyTierMapper;
import tj.radolfa.infrastructure.persistence.repository.LoyaltyTierRepository;

import java.util.List;
import java.util.Optional;

@Component
public class LoyaltyTierRepositoryAdapter implements LoadLoyaltyTierPort, SaveLoyaltyTierPort {

    private final LoyaltyTierRepository repository;
    private final LoyaltyTierMapper mapper;

    public LoyaltyTierRepositoryAdapter(LoyaltyTierRepository repository,
                                        LoyaltyTierMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<LoyaltyTier> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LoyaltyTier> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<LoyaltyTier> findAll() {
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public LoyaltyTier save(LoyaltyTier tier) {
        LoyaltyTierEntity entity = mapper.toEntity(tier);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<LoyaltyTier> saveAll(List<LoyaltyTier> tiers) {
        List<LoyaltyTierEntity> entities = tiers.stream()
                .map(mapper::toEntity)
                .toList();
        return repository.saveAll(entities)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
