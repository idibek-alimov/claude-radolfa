package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.order.GetAdminOrderSummaryUseCase;
import tj.radolfa.application.ports.out.AdminOrderSummary;
import tj.radolfa.infrastructure.web.dto.AdminOrderSummaryDto;
import tj.radolfa.infrastructure.web.dto.RecentOrderDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin — Orders", description = "Admin-only order management endpoints")
public class AdminOrderController {

    private final GetAdminOrderSummaryUseCase getAdminOrderSummaryUseCase;

    public AdminOrderController(GetAdminOrderSummaryUseCase getAdminOrderSummaryUseCase) {
        this.getAdminOrderSummaryUseCase = getAdminOrderSummaryUseCase;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin order summary — totals, revenue, and last 10 orders (ADMIN only)")
    public ResponseEntity<AdminOrderSummaryDto> getSummary() {
        AdminOrderSummary summary = getAdminOrderSummaryUseCase.execute();
        return ResponseEntity.ok(toDto(summary));
    }

    private AdminOrderSummaryDto toDto(AdminOrderSummary summary) {
        List<RecentOrderDto> recentOrders = summary.recentOrders().stream()
                .map(o -> new RecentOrderDto(
                        o.orderId(),
                        o.userPhone(),
                        o.totalAmount(),
                        o.status(),
                        o.createdAt()))
                .toList();

        return new AdminOrderSummaryDto(
                summary.totalOrders(),
                summary.todayOrders(),
                summary.revenueToday(),
                summary.revenueThisMonth(),
                recentOrders);
    }
}
