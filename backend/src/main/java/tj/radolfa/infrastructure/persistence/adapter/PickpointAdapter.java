package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.SavePickpointPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.infrastructure.persistence.entity.PickpointEntity;
import tj.radolfa.infrastructure.persistence.repository.PickpointRepository;

import java.util.List;
import java.util.Optional;

@Component
public class PickpointAdapter implements LoadPickpointPort, SavePickpointPort {

    private final PickpointRepository repo;

    public PickpointAdapter(PickpointRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Pickpoint> findAll() {
        return repo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Pickpoint> findAllActive() {
        return repo.findAllByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Pickpoint> findById(Long id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public Pickpoint save(String name, String address) {
        PickpointEntity entity = new PickpointEntity();
        entity.setName(name);
        entity.setAddress(address);
        entity.setActive(true);
        return toDomain(repo.save(entity));
    }

    @Override
    public Pickpoint update(Long id, String name, String address, boolean active) {
        PickpointEntity entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pickpoint not found: " + id));
        entity.setName(name);
        entity.setAddress(address);
        entity.setActive(active);
        return toDomain(repo.save(entity));
    }

    private Pickpoint toDomain(PickpointEntity e) {
        return new Pickpoint(e.getId(), e.getName(), e.getAddress(), e.isActive());
    }
}
