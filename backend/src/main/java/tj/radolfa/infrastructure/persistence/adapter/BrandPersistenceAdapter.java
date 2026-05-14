package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadBrandPort;
import tj.radolfa.infrastructure.persistence.repository.BrandRepository;

import java.util.Optional;

@Component
public class BrandPersistenceAdapter implements LoadBrandPort {

    private final BrandRepository brandRepo;

    public BrandPersistenceAdapter(BrandRepository brandRepo) {
        this.brandRepo = brandRepo;
    }

    @Override
    public Optional<BrandView> findById(Long id) {
        return brandRepo.findById(id)
                .map(e -> new BrandView(e.getId(), e.getName()));
    }
}
