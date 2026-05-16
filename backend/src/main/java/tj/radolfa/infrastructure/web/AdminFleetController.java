package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.BulkReassignOrdersUseCase;
import tj.radolfa.application.ports.in.order.GetCourierFleetSummaryUseCase;
import tj.radolfa.infrastructure.web.dto.CourierFleetDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/fleet")
@Tag(name = "Admin — Fleet", description = "Courier fleet management endpoints")
public class AdminFleetController {

    private final GetCourierFleetSummaryUseCase getCourierFleetSummaryUseCase;
    private final BulkReassignOrdersUseCase     bulkReassignOrdersUseCase;

    public AdminFleetController(GetCourierFleetSummaryUseCase getCourierFleetSummaryUseCase,
                                BulkReassignOrdersUseCase bulkReassignOrdersUseCase) {
        this.getCourierFleetSummaryUseCase = getCourierFleetSummaryUseCase;
        this.bulkReassignOrdersUseCase     = bulkReassignOrdersUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get fleet summary — per-courier delivery counts (ADMIN only)")
    public ResponseEntity<List<CourierFleetDto>> getFleetSummary() {
        return ResponseEntity.ok(
                getCourierFleetSummaryUseCase.execute().stream()
                        .map(CourierFleetDto::from)
                        .toList());
    }

    @PostMapping("/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk-reassign orders to a different courier (ADMIN only)")
    public ResponseEntity<Void> bulkReassign(@Valid @RequestBody BulkReassignRequest body) {
        bulkReassignOrdersUseCase.execute(
                new BulkReassignOrdersUseCase.Command(body.orderIds(), body.newCourierId()));
        return ResponseEntity.noContent().build();
    }

    record BulkReassignRequest(
            @NotEmpty List<Long> orderIds,
            @NotNull  Long       newCourierId) {}
}
