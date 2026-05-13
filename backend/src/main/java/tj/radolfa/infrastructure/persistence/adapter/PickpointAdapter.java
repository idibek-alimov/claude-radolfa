package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.command.CreatePickpointCommand;
import tj.radolfa.application.command.UpdatePickpointCommand;
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
    public List<Pickpoint> findAll(String search) {
        boolean hasSearch = search != null && !search.isBlank();
        List<PickpointEntity> entities = hasSearch
                ? repo.searchAll(search.trim())
                : repo.findAllByOrderByActiveDescNameAsc();
        return entities.stream().map(this::toDomain).toList();
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
    public Pickpoint save(CreatePickpointCommand cmd) {
        PickpointEntity entity = new PickpointEntity();
        entity.setName(cmd.name());
        entity.setAddress(cmd.address());
        entity.setActive(true);
        entity.setLatitude(cmd.latitude());
        entity.setLongitude(cmd.longitude());
        entity.setHasParking(cmd.hasParking());
        entity.setHasFittingRoom(cmd.hasFittingRoom());
        entity.setHasCardPayment(cmd.hasCardPayment());
        entity.setWheelchairAccessible(cmd.wheelchairAccessible());
        return toDomain(repo.save(entity));
    }

    @Override
    public Pickpoint update(UpdatePickpointCommand cmd) {
        PickpointEntity entity = repo.findById(cmd.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickpoint not found: " + cmd.id()));
        entity.setName(cmd.name());
        entity.setAddress(cmd.address());
        entity.setActive(cmd.active());
        entity.setLatitude(cmd.latitude());
        entity.setLongitude(cmd.longitude());
        entity.setHasParking(cmd.hasParking());
        entity.setHasFittingRoom(cmd.hasFittingRoom());
        entity.setHasCardPayment(cmd.hasCardPayment());
        entity.setWheelchairAccessible(cmd.wheelchairAccessible());
        return toDomain(repo.save(entity));
    }

    private Pickpoint toDomain(PickpointEntity e) {
        return new Pickpoint(
                e.getId(), e.getName(), e.getAddress(), e.isActive(),
                e.getLatitude(), e.getLongitude(),
                e.isHasParking(), e.isHasFittingRoom(),
                e.isHasCardPayment(), e.isWheelchairAccessible()
        );
    }
}
