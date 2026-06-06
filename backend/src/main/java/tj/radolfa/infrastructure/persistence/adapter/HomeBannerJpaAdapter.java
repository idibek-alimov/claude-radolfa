package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadHomeBannerPort;
import tj.radolfa.application.ports.out.SaveHomeBannerPort;
import tj.radolfa.domain.model.HomeBanner;
import tj.radolfa.infrastructure.persistence.mappers.HomeBannerMapper;
import tj.radolfa.infrastructure.persistence.repository.HomeBannerRepository;

import java.util.List;
import java.util.Optional;

@Component
public class HomeBannerJpaAdapter implements LoadHomeBannerPort, SaveHomeBannerPort {

    private final HomeBannerRepository repository;
    private final HomeBannerMapper mapper;

    public HomeBannerJpaAdapter(HomeBannerRepository repository, HomeBannerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<HomeBanner> findActive() {
        return repository.findByActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<HomeBanner> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<HomeBanner> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public HomeBanner save(HomeBanner banner) {
        return mapper.toDomain(repository.save(mapper.toEntity(banner)));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
