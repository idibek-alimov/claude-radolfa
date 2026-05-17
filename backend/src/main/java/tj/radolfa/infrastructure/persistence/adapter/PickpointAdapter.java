package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.command.CreatePickpointCommand;
import tj.radolfa.application.command.UpdatePickpointCommand;
import tj.radolfa.application.ports.out.LoadPickpointHoursPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.SavePickpointHoursPort;
import tj.radolfa.application.ports.out.SavePickpointPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.PickpointHours;
import tj.radolfa.infrastructure.persistence.entity.PickpointEntity;
import tj.radolfa.infrastructure.persistence.entity.PickpointHoursEntity;
import tj.radolfa.infrastructure.persistence.repository.PickpointHoursRepository;
import tj.radolfa.infrastructure.persistence.repository.PickpointRepository;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PickpointAdapter implements LoadPickpointPort, SavePickpointPort,
        LoadPickpointHoursPort, SavePickpointHoursPort {

    private final PickpointRepository repo;
    private final PickpointHoursRepository hoursRepo;

    public PickpointAdapter(PickpointRepository repo, PickpointHoursRepository hoursRepo) {
        this.repo = repo;
        this.hoursRepo = hoursRepo;
    }

    // ── LoadPickpointPort ─────────────────────────────────────────────────────

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
    public List<Pickpoint> findAllByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return repo.findAllById(ids).stream().map(this::toDomain).toList();
    }

    // ── SavePickpointPort ─────────────────────────────────────────────────────

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
        // timezone and temporarily_closed default via DB (NULL / FALSE)
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
        entity.setTimezone(cmd.timezone());
        entity.setTemporarilyClosed(cmd.temporarilyClosed());
        return toDomain(repo.save(entity));
    }

    // ── LoadPickpointHoursPort ────────────────────────────────────────────────

    @Override
    public List<PickpointHours> findByPickpointId(Long pickpointId) {
        return hoursRepo.findAllByPickpointIdOrderByDayOfWeek(pickpointId)
                .stream().map(this::hoursToDomain).toList();
    }

    @Override
    public Map<Long, List<PickpointHours>> findByPickpointIds(Collection<Long> ids) {
        return hoursRepo
                .findAllByPickpointIdInOrderByPickpointIdAscDayOfWeekAsc(ids)
                .stream()
                .map(this::hoursToDomain)
                .collect(Collectors.groupingBy(PickpointHours::pickpointId));
    }

    // ── SavePickpointHoursPort ────────────────────────────────────────────────

    @Override
    public List<PickpointHours> replaceAll(Long pickpointId, List<PickpointHours> hours) {
        hoursRepo.deleteAllByPickpointId(pickpointId);
        List<PickpointHoursEntity> entities = hours.stream()
                .map(h -> {
                    PickpointHoursEntity e = new PickpointHoursEntity();
                    e.setPickpointId(pickpointId);
                    e.setDayOfWeek((short) h.dayOfWeek());
                    e.setOpenTime(h.openTime());
                    e.setCloseTime(h.closeTime());
                    return e;
                }).toList();
        return hoursRepo.saveAll(entities).stream().map(this::hoursToDomain).toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Pickpoint toDomain(PickpointEntity e) {
        return new Pickpoint(
                e.getId(), e.getName(), e.getAddress(), e.isActive(),
                e.getLatitude(), e.getLongitude(),
                e.isHasParking(), e.isHasFittingRoom(),
                e.isHasCardPayment(), e.isWheelchairAccessible(),
                e.getTimezone(), e.isTemporarilyClosed()
        );
    }

    private PickpointHours hoursToDomain(PickpointHoursEntity e) {
        return new PickpointHours(
                e.getId(), e.getPickpointId(), e.getDayOfWeek(),
                e.getOpenTime(), e.getCloseTime()
        );
    }

    // ── Open-now computation (called by controllers for response projection) ──

    public static boolean computeIsOpenNow(Pickpoint p, List<PickpointHours> hours) {
        if (p.temporarilyClosed()) return false;
        String tz = (p.timezone() != null && !p.timezone().isBlank()) ? p.timezone() : "UTC";
        ZonedDateTime now;
        try {
            now = ZonedDateTime.now(ZoneId.of(tz));
        } catch (DateTimeException e) {
            now = ZonedDateTime.now(ZoneId.of("UTC"));
        }
        int today = now.getDayOfWeek().getValue();
        LocalTime t = now.toLocalTime();
        return hours.stream()
                .filter(h -> h.dayOfWeek() == today)
                .anyMatch(h -> !t.isBefore(h.openTime()) && t.isBefore(h.closeTime()));
    }
}
