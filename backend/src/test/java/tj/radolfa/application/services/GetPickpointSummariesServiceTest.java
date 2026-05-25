package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.GetPickpointSummariesUseCase.PickpointSummary;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadPickpointStatsPort;
import tj.radolfa.domain.model.Pickpoint;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetPickpointSummariesServiceTest {

    // ── Fakes ────────────────────────────────────────────────────────────────

    static Pickpoint pickpoint(Long id, String name) {
        return new Pickpoint(id, name, "Addr", true, null, null,
                false, false, false, false, "UTC", false);
    }

    static LoadPickpointPort pickpointPort(List<Pickpoint> active) {
        return new LoadPickpointPort() {
            @Override public List<Pickpoint> findAll(String search) { return active; }
            @Override public List<Pickpoint> findAllActive() { return active; }
            @Override public Optional<Pickpoint> findById(Long id) {
                return active.stream().filter(p -> p.id().equals(id)).findFirst();
            }
            @Override public List<Pickpoint> findAllByIds(Collection<Long> ids) {
                return active.stream().filter(p -> ids.contains(p.id())).toList();
            }
        };
    }

    static LoadPickpointStatsPort statsPort(List<LoadPickpointStatsPort.OrderCountRow> orderRows,
                                            List<LoadPickpointStatsPort.CustomerReturnCountRow> returnRows) {
        return new LoadPickpointStatsPort() {
            @Override public List<OrderCountRow> countOrdersByPickpointAndStatus(Instant cutoff) { return orderRows; }
            @Override public List<CustomerReturnCountRow> countCustomerReturnsReceived() { return returnRows; }
        };
    }

    GetPickpointSummariesService service(List<Pickpoint> active,
                                         List<LoadPickpointStatsPort.OrderCountRow> orderRows,
                                         List<LoadPickpointStatsPort.CustomerReturnCountRow> returnRows) {
        return new GetPickpointSummariesService(statsPort(orderRows, returnRows), pickpointPort(active));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Two pickpoints with full stats → summaries returned sorted by name")
    void twoPickpointsFullStats() {
        var active = List.of(pickpoint(2L, "Beta"), pickpoint(1L, "Alpha"));
        var orderRows = List.of(
                new LoadPickpointStatsPort.OrderCountRow(1L, 3, 5, 2, 1),
                new LoadPickpointStatsPort.OrderCountRow(2L, 1, 0, 0, 0));
        var returnRows = List.of(
                new LoadPickpointStatsPort.CustomerReturnCountRow(1L, 4),
                new LoadPickpointStatsPort.CustomerReturnCountRow(2L, 2));

        List<PickpointSummary> result = service(active, orderRows, returnRows).execute();

        assertEquals(2, result.size());
        // sorted by name: Alpha first, Beta second
        PickpointSummary alpha = result.get(0);
        assertEquals("Alpha", alpha.name());
        assertEquals(3, alpha.incoming());
        assertEquals(5, alpha.awaitingPickup());
        assertEquals(2, alpha.overdue());
        assertEquals(1, alpha.returnInProgress());
        assertEquals(4, alpha.customerReturns());

        PickpointSummary beta = result.get(1);
        assertEquals("Beta", beta.name());
        assertEquals(1, beta.incoming());
        assertEquals(0, beta.awaitingPickup());
        assertEquals(0, beta.overdue());
        assertEquals(2, beta.customerReturns());
    }

    @Test
    @DisplayName("Pickpoint with no order or return rows → all zeros")
    void pickpointWithNoStats() {
        var active = List.of(pickpoint(1L, "Empty"));

        List<PickpointSummary> result = service(active, List.of(), List.of()).execute();

        assertEquals(1, result.size());
        PickpointSummary summary = result.get(0);
        assertEquals(0, summary.incoming());
        assertEquals(0, summary.awaitingPickup());
        assertEquals(0, summary.overdue());
        assertEquals(0, summary.returnInProgress());
        assertEquals(0, summary.customerReturns());
    }

    @Test
    @DisplayName("No active pickpoints → empty list")
    void noActivePickpoints() {
        var result = service(List.of(), List.of(), List.of()).execute();
        assertTrue(result.isEmpty());
    }
}
