package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.domain.model.Warehouse;
import tj.radolfa.infrastructure.persistence.mappers.WarehouseMapper;
import tj.radolfa.infrastructure.persistence.repository.WarehouseRepository;

import java.util.Optional;

@Component
public class WarehouseJpaAdapter implements LoadWarehousePort {

    private final WarehouseRepository repository;
    private final WarehouseMapper mapper;

    public WarehouseJpaAdapter(WarehouseRepository repository, WarehouseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Warehouse findDefault() {
        return repository.findByIsDefaultTrue()
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("No default warehouse configured"));
    }

    @Override
    public Optional<Warehouse> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
