package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.AdminOrderSummary;
import tj.radolfa.application.ports.out.AdminOrderSummaryPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link GetAdminOrderSummaryService}.
 *
 * <p>Uses a hand-written in-memory fake for {@link AdminOrderSummaryPort}.
 * No Spring context, no Mockito, no database.
 */
class GetAdminOrderSummaryServiceTest {

    // ── Fake ────────────────────────────────────────────────────────────────────

    static class FakeAdminOrderSummaryPort implements AdminOrderSummaryPort {

        private final AdminOrderSummary result;

        FakeAdminOrderSummaryPort(AdminOrderSummary result) {
            this.result = result;
        }

        @Override
        public AdminOrderSummary load() {
            return result;
        }
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────────

    private static final AdminOrderSummary SAMPLE = new AdminOrderSummary(
            42L,
            3L,
            new BigDecimal("15000.00"),
            new BigDecimal("350000.00"),
            List.of(
                    new AdminOrderSummary.RecentOrder(
                            1L, "+992901234567",
                            new BigDecimal("5000.00"), "PAID",
                            Instant.parse("2026-04-07T10:00:00Z")),
                    new AdminOrderSummary.RecentOrder(
                            2L, "+992907654321",
                            new BigDecimal("10000.00"), "DELIVERED",
                            Instant.parse("2026-04-07T08:00:00Z"))
            )
    );

    private GetAdminOrderSummaryService service;

    @BeforeEach
    void setUp() {
        service = new GetAdminOrderSummaryService(new FakeAdminOrderSummaryPort(SAMPLE));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute() returns the summary from the port unchanged")
    void execute_returnsSummaryFromPort() {
        AdminOrderSummary result = service.execute();

        assertThat(result.totalOrders()).isEqualTo(42L);
        assertThat(result.todayOrders()).isEqualTo(3L);
        assertThat(result.revenueToday()).isEqualByComparingTo("15000.00");
        assertThat(result.revenueThisMonth()).isEqualByComparingTo("350000.00");
        assertThat(result.recentOrders()).hasSize(2);
    }

    @Test
    @DisplayName("recentOrders are preserved in order")
    void execute_preservesRecentOrderOrder() {
        AdminOrderSummary result = service.execute();

        assertThat(result.recentOrders().get(0).orderId()).isEqualTo(1L);
        assertThat(result.recentOrders().get(0).userPhone()).isEqualTo("+992901234567");
        assertThat(result.recentOrders().get(0).status()).isEqualTo("PAID");

        assertThat(result.recentOrders().get(1).orderId()).isEqualTo(2L);
        assertThat(result.recentOrders().get(1).status()).isEqualTo("DELIVERED");
    }

    @Test
    @DisplayName("Empty recentOrders is returned unchanged")
    void execute_emptyRecentOrders() {
        AdminOrderSummary empty = new AdminOrderSummary(
                0L, 0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        );
        service = new GetAdminOrderSummaryService(new FakeAdminOrderSummaryPort(empty));

        AdminOrderSummary result = service.execute();

        assertThat(result.totalOrders()).isZero();
        assertThat(result.recentOrders()).isEmpty();
    }
}
