package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.command.CreatePickpointCommand;
import tj.radolfa.application.command.UpdatePickpointCommand;
import tj.radolfa.application.ports.in.CreatePickpointUseCase;
import tj.radolfa.application.ports.in.GetPickpointHoursUseCase;
import tj.radolfa.application.ports.in.ListAllPickpointsUseCase;
import tj.radolfa.application.ports.in.UpdatePickpointHoursUseCase;
import tj.radolfa.application.ports.in.UpdatePickpointUseCase;
import tj.radolfa.application.ports.out.LoadPickpointHoursPort;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.PickpointHours;
import tj.radolfa.infrastructure.persistence.adapter.PickpointAdapter;
import tj.radolfa.infrastructure.web.PickpointController.PickpointResponse;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/pickpoints")
@Tag(name = "Admin — Pickpoints", description = "Pickpoint registry management")
public class AdminPickpointController {

    private final ListAllPickpointsUseCase listAllPickpointsUseCase;
    private final CreatePickpointUseCase createPickpointUseCase;
    private final UpdatePickpointUseCase updatePickpointUseCase;
    private final GetPickpointHoursUseCase getPickpointHoursUseCase;
    private final UpdatePickpointHoursUseCase updatePickpointHoursUseCase;
    private final LoadPickpointHoursPort loadPickpointHoursPort;

    public AdminPickpointController(ListAllPickpointsUseCase listAllPickpointsUseCase,
                                    CreatePickpointUseCase createPickpointUseCase,
                                    UpdatePickpointUseCase updatePickpointUseCase,
                                    GetPickpointHoursUseCase getPickpointHoursUseCase,
                                    UpdatePickpointHoursUseCase updatePickpointHoursUseCase,
                                    LoadPickpointHoursPort loadPickpointHoursPort) {
        this.listAllPickpointsUseCase = listAllPickpointsUseCase;
        this.createPickpointUseCase = createPickpointUseCase;
        this.updatePickpointUseCase = updatePickpointUseCase;
        this.getPickpointHoursUseCase = getPickpointHoursUseCase;
        this.updatePickpointHoursUseCase = updatePickpointHoursUseCase;
        this.loadPickpointHoursPort = loadPickpointHoursPort;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all pickpoints", description = "Returns all pickpoints including inactive ones. ADMIN only.")
    @ApiResponse(responseCode = "200", description = "Pickpoint list returned")
    public ResponseEntity<List<PickpointResponse>> listAll(
            @RequestParam(required = false) String search) {
        List<Pickpoint> pickpoints = listAllPickpointsUseCase.execute(search);
        Map<Long, List<PickpointHours>> hoursMap = loadPickpointHoursPort.findByPickpointIds(
                pickpoints.stream().map(Pickpoint::id).toList());
        List<PickpointResponse> body = pickpoints.stream()
                .map(p -> PickpointResponse.from(p, hoursMap.getOrDefault(p.id(), List.of())))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a pickpoint", description = "Creates a new active pickup location. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pickpoint created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public ResponseEntity<PickpointResponse> create(@Valid @RequestBody CreatePickpointRequest request) {
        Pickpoint created = createPickpointUseCase.execute(new CreatePickpointCommand(
                request.name(), request.address(),
                request.latitude(), request.longitude(),
                request.hasParking(), request.hasFittingRoom(),
                request.hasCardPayment(), request.wheelchairAccessible()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PickpointResponse.from(created, List.of()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a pickpoint", description = "Updates name, address, active status, coordinates, amenities, timezone, and/or temporarily-closed flag. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pickpoint updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Pickpoint not found")
    })
    public ResponseEntity<PickpointResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePickpointRequest request) {
        Pickpoint updated = updatePickpointUseCase.execute(new UpdatePickpointCommand(
                id, request.name(), request.address(), request.active(),
                request.latitude(), request.longitude(),
                request.hasParking(), request.hasFittingRoom(),
                request.hasCardPayment(), request.wheelchairAccessible(),
                request.timezone(), request.temporarilyClosed()));
        List<PickpointHours> hours = loadPickpointHoursPort
                .findByPickpointIds(List.of(id))
                .getOrDefault(id, List.of());
        return ResponseEntity.ok(PickpointResponse.from(updated, hours));
    }

    @GetMapping("/{id}/hours")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pickpoint hours", description = "Returns the weekly schedule for a pickpoint. ADMIN only.")
    @ApiResponse(responseCode = "200", description = "Hours returned")
    public ResponseEntity<List<PickpointHoursResponse>> getHours(@PathVariable Long id) {
        return ResponseEntity.ok(
                getPickpointHoursUseCase.execute(id).stream()
                        .map(PickpointHoursResponse::from).toList());
    }

    @PutMapping("/{id}/hours")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replace pickpoint hours", description = "Atomically replaces the full weekly schedule. Send only enabled days. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hours updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Pickpoint not found")
    })
    public ResponseEntity<List<PickpointHoursResponse>> updateHours(
            @PathVariable Long id,
            @RequestBody @Valid List<PickpointHoursRequest> body) {
        List<PickpointHours> hours = body.stream()
                .map(r -> new PickpointHours(null, id, r.dayOfWeek(),
                        LocalTime.parse(r.openTime()), LocalTime.parse(r.closeTime())))
                .toList();
        return ResponseEntity.ok(
                updatePickpointHoursUseCase.execute(id, hours).stream()
                        .map(PickpointHoursResponse::from).toList());
    }

    // ── Request / Response records ────────────────────────────────────────────

    public record CreatePickpointRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 255, message = "Name must not exceed 255 characters")
            String name,
            @NotBlank(message = "Address is required")
            String address,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            Double longitude,
            boolean hasParking,
            boolean hasFittingRoom,
            boolean hasCardPayment,
            boolean wheelchairAccessible) {
    }

    public record UpdatePickpointRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 255, message = "Name must not exceed 255 characters")
            String name,
            @NotBlank(message = "Address is required")
            String address,
            boolean active,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            Double longitude,
            boolean hasParking,
            boolean hasFittingRoom,
            boolean hasCardPayment,
            boolean wheelchairAccessible,
            @Size(max = 50, message = "Timezone must not exceed 50 characters")
            String timezone,
            boolean temporarilyClosed) {
    }

    public record PickpointHoursRequest(
            @Min(value = 1, message = "dayOfWeek must be between 1 and 7")
            @Max(value = 7, message = "dayOfWeek must be between 1 and 7")
            int dayOfWeek,
            @NotBlank(message = "openTime is required")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "openTime must match HH:mm")
            String openTime,
            @NotBlank(message = "closeTime is required")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "closeTime must match HH:mm")
            String closeTime) {
    }

    public record PickpointHoursResponse(Long id, int dayOfWeek, String openTime, String closeTime) {
        public static PickpointHoursResponse from(PickpointHours h) {
            return new PickpointHoursResponse(h.id(), h.dayOfWeek(),
                    h.openTime().toString(), h.closeTime().toString());
        }
    }
}
