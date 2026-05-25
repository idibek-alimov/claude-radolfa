package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.CourierFleetEntry;
import tj.radolfa.application.ports.out.LoadCourierOrderStatsPort;
import tj.radolfa.application.ports.out.LoadCourierOrderStatsPort.CourierOrderStats;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetCourierFleetSummaryServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static User courier(long id, String name, VehicleType vehicleType) {
        return new User(id, new PhoneNumber("992000000" + id), UserRole.COURIER,
                name, null, LoyaltyProfile.empty(), true, 1L,
                vehicleType, BigDecimal.valueOf(100), null, null, null, null, null);
    }

    static LoadUserPort userPort(List<User> couriers) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) { return Optional.empty(); }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(UserRole r) {
                return r == UserRole.COURIER ? couriers : List.of();
            }
        };
    }

    static LoadCourierOrderStatsPort statsPort(Map<Long, CourierOrderStats> stats) {
        return todayStart -> stats;
    }

    static GetCourierFleetSummaryService service(List<User> couriers,
                                                  Map<Long, CourierOrderStats> stats) {
        return new GetCourierFleetSummaryService(userPort(couriers), statsPort(stats));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Two couriers with stats, one with no orders — all three appear; no-order courier has zeros")
    void fleetSummary_mergesStatsAndFillsZeros() {
        User alice = courier(1L, "Alice", VehicleType.MOTORCYCLE);
        User bob   = courier(2L, "Bob",   VehicleType.CAR);
        User carl  = courier(3L, "Carl",  VehicleType.BICYCLE);

        Map<Long, CourierOrderStats> stats = Map.of(
                1L, new CourierOrderStats(5L, 2L, 1L),
                2L, new CourierOrderStats(3L, 1L, 0L));

        List<CourierFleetEntry> result = service(List.of(alice, bob, carl), stats).execute();

        assertEquals(3, result.size());

        CourierFleetEntry aliceEntry = result.stream().filter(e -> e.courierId().equals(1L)).findFirst().orElseThrow();
        assertEquals(5L, aliceEntry.deliveredToday());
        assertEquals(2L, aliceEntry.inTransit());
        assertEquals(1L, aliceEntry.attempted());

        CourierFleetEntry carlEntry = result.stream().filter(e -> e.courierId().equals(3L)).findFirst().orElseThrow();
        assertEquals(0L, carlEntry.deliveredToday());
        assertEquals(0L, carlEntry.inTransit());
        assertEquals(0L, carlEntry.attempted());
    }

    @Test
    @DisplayName("Result is sorted alphabetically by courier name")
    void fleetSummary_sortedByNameAscending() {
        User zed   = courier(1L, "Zed",   VehicleType.CAR);
        User alice = courier(2L, "Alice", VehicleType.BICYCLE);
        User bob   = courier(3L, "Bob",   VehicleType.MOTORCYCLE);

        List<CourierFleetEntry> result = service(List.of(zed, alice, bob), Map.of()).execute();

        assertEquals("Alice", result.get(0).name());
        assertEquals("Bob",   result.get(1).name());
        assertEquals("Zed",   result.get(2).name());
    }

    @Test
    @DisplayName("No couriers registered → empty list")
    void noCouriers_returnsEmpty() {
        List<CourierFleetEntry> result = service(List.of(), Map.of()).execute();
        assertTrue(result.isEmpty());
    }
}
