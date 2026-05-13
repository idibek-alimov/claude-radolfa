package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.ListActivePickpointsUseCase;
import tj.radolfa.domain.model.Pickpoint;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pickpoints")
@Tag(name = "Pickpoints", description = "Public pickup location list for checkout")
public class PickpointController {

    private final ListActivePickpointsUseCase listActivePickpointsUseCase;

    public PickpointController(ListActivePickpointsUseCase listActivePickpointsUseCase) {
        this.listActivePickpointsUseCase = listActivePickpointsUseCase;
    }

    @GetMapping
    @Operation(summary = "List active pickpoints", description = "Returns all active pickup locations available for checkout.")
    @ApiResponse(responseCode = "200", description = "Pickpoint list returned")
    public ResponseEntity<List<PickpointResponse>> listActive() {
        List<PickpointResponse> body = listActivePickpointsUseCase.execute().stream()
                .map(PickpointResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    public record PickpointResponse(
            Long id,
            String name,
            String address,
            boolean active,
            Double latitude,
            Double longitude,
            boolean hasParking,
            boolean hasFittingRoom,
            boolean hasCardPayment,
            boolean wheelchairAccessible
    ) {
        public static PickpointResponse from(Pickpoint p) {
            return new PickpointResponse(
                    p.id(), p.name(), p.address(), p.active(),
                    p.latitude(), p.longitude(),
                    p.hasParking(), p.hasFittingRoom(),
                    p.hasCardPayment(), p.wheelchairAccessible());
        }
    }
}
