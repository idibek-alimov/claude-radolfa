package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.CreatePickpointUseCase;
import tj.radolfa.application.ports.in.ListAllPickpointsUseCase;
import tj.radolfa.application.ports.in.UpdatePickpointUseCase;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.infrastructure.web.PickpointController.PickpointResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/pickpoints")
@Tag(name = "Admin — Pickpoints", description = "Pickpoint registry management")
public class AdminPickpointController {

    private final ListAllPickpointsUseCase listAllPickpointsUseCase;
    private final CreatePickpointUseCase createPickpointUseCase;
    private final UpdatePickpointUseCase updatePickpointUseCase;

    public AdminPickpointController(ListAllPickpointsUseCase listAllPickpointsUseCase,
                                    CreatePickpointUseCase createPickpointUseCase,
                                    UpdatePickpointUseCase updatePickpointUseCase) {
        this.listAllPickpointsUseCase = listAllPickpointsUseCase;
        this.createPickpointUseCase = createPickpointUseCase;
        this.updatePickpointUseCase = updatePickpointUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all pickpoints", description = "Returns all pickpoints including inactive ones. ADMIN only.")
    @ApiResponse(responseCode = "200", description = "Pickpoint list returned")
    public ResponseEntity<List<PickpointResponse>> listAll() {
        List<PickpointResponse> body = listAllPickpointsUseCase.execute().stream()
                .map(PickpointResponse::from)
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
        Pickpoint created = createPickpointUseCase.execute(request.name(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(PickpointResponse.from(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a pickpoint", description = "Updates name, address, and/or active status. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pickpoint updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Pickpoint not found")
    })
    public ResponseEntity<PickpointResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePickpointRequest request) {
        Pickpoint updated = updatePickpointUseCase.execute(id, request.name(), request.address(), request.active());
        return ResponseEntity.ok(PickpointResponse.from(updated));
    }

    public record CreatePickpointRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 255, message = "Name must not exceed 255 characters")
            String name,
            @NotBlank(message = "Address is required")
            String address) {
    }

    public record UpdatePickpointRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 255, message = "Name must not exceed 255 characters")
            String name,
            @NotBlank(message = "Address is required")
            String address,
            boolean active) {
    }
}
