package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.user.*;
import tj.radolfa.domain.model.VehicleType;
import tj.radolfa.infrastructure.web.dto.CourierSummaryDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserManagementController {

    private final CreateCourierUserUseCase     createCourierUserUseCase;
    private final CreatePickpointStaffUseCase  createPickpointStaffUseCase;
    private final UpdateCourierDetailsUseCase  updateCourierDetailsUseCase;
    private final ReassignPickpointStaffUseCase reassignPickpointStaffUseCase;
    private final ListCouriersUseCase          listCouriersUseCase;

    public AdminUserManagementController(CreateCourierUserUseCase createCourierUserUseCase,
                                         CreatePickpointStaffUseCase createPickpointStaffUseCase,
                                         UpdateCourierDetailsUseCase updateCourierDetailsUseCase,
                                         ReassignPickpointStaffUseCase reassignPickpointStaffUseCase,
                                         ListCouriersUseCase listCouriersUseCase) {
        this.createCourierUserUseCase    = createCourierUserUseCase;
        this.createPickpointStaffUseCase = createPickpointStaffUseCase;
        this.updateCourierDetailsUseCase = updateCourierDetailsUseCase;
        this.reassignPickpointStaffUseCase = reassignPickpointStaffUseCase;
        this.listCouriersUseCase         = listCouriersUseCase;
    }

    // ── Request records ────────────────────────────────────────────────────────

    record CreateCourierRequest(
            @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}") String phone,
            @NotBlank String name,
            @NotNull VehicleType vehicleType,
            @NotNull @DecimalMin("0.1") BigDecimal maxPayloadKg,
            Integer maxLengthCm,
            Integer maxWidthCm,
            Integer maxHeightCm) {}

    record CreatePickpointStaffRequest(
            @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}") String phone,
            @NotBlank String name,
            @NotNull Long pickpointId) {}

    record UpdateCourierDetailsRequest(
            @NotNull VehicleType vehicleType,
            @NotNull @DecimalMin("0.1") BigDecimal maxPayloadKg,
            Integer maxLengthCm,
            Integer maxWidthCm,
            Integer maxHeightCm) {}

    record ReassignPickpointRequest(@NotNull Long pickpointId) {}

    // ── Handlers ───────────────────────────────────────────────────────────────

    @PostMapping("/couriers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> createCourier(
            @Valid @RequestBody CreateCourierRequest body) {
        Long id = createCourierUserUseCase.execute(new CreateCourierUserUseCase.Command(
                body.phone(), body.name(), body.vehicleType(),
                body.maxPayloadKg(), body.maxLengthCm(), body.maxWidthCm(), body.maxHeightCm()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PostMapping("/pickpoint-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> createPickpointStaff(
            @Valid @RequestBody CreatePickpointStaffRequest body) {
        Long id = createPickpointStaffUseCase.execute(new CreatePickpointStaffUseCase.Command(
                body.phone(), body.name(), body.pickpointId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/couriers")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<CourierSummaryDto>> listCouriers() {
        List<CourierSummaryDto> couriers = listCouriersUseCase.execute().stream()
                .map(CourierSummaryDto::from)
                .toList();
        return ResponseEntity.ok(couriers);
    }

    @PatchMapping("/{id}/courier-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateCourierDetails(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourierDetailsRequest body) {
        updateCourierDetailsUseCase.execute(new UpdateCourierDetailsUseCase.Command(
                id, body.vehicleType(), body.maxPayloadKg(),
                body.maxLengthCm(), body.maxWidthCm(), body.maxHeightCm()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pickpoint-assignment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reassignPickpointStaff(
            @PathVariable Long id,
            @Valid @RequestBody ReassignPickpointRequest body) {
        reassignPickpointStaffUseCase.execute(id, body.pickpointId());
        return ResponseEntity.noContent().build();
    }
}
