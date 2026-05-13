package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.ListActivePickpointsUseCase;
import tj.radolfa.application.ports.out.LoadPickpointHoursPort;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.PickpointHours;
import tj.radolfa.infrastructure.persistence.adapter.PickpointAdapter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pickpoints")
@Tag(name = "Pickpoints", description = "Public pickup location list for checkout")
public class PickpointController {

    private final ListActivePickpointsUseCase listActivePickpointsUseCase;
    private final LoadPickpointHoursPort loadPickpointHoursPort;

    public PickpointController(ListActivePickpointsUseCase listActivePickpointsUseCase,
                               LoadPickpointHoursPort loadPickpointHoursPort) {
        this.listActivePickpointsUseCase = listActivePickpointsUseCase;
        this.loadPickpointHoursPort = loadPickpointHoursPort;
    }

    @GetMapping
    @Operation(summary = "List active pickpoints", description = "Returns all active pickup locations available for checkout.")
    @ApiResponse(responseCode = "200", description = "Pickpoint list returned")
    public ResponseEntity<List<PickpointResponse>> listActive() {
        List<Pickpoint> pickpoints = listActivePickpointsUseCase.execute();
        Map<Long, List<PickpointHours>> hoursMap = loadPickpointHoursPort.findByPickpointIds(
                pickpoints.stream().map(Pickpoint::id).toList());
        List<PickpointResponse> body = pickpoints.stream()
                .map(p -> PickpointResponse.from(p, hoursMap.getOrDefault(p.id(), List.of())))
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
            boolean wheelchairAccessible,
            String timezone,
            boolean temporarilyClosed,
            boolean isOpenNow
    ) {
        public static PickpointResponse from(Pickpoint p, List<PickpointHours> hours) {
            return new PickpointResponse(
                    p.id(), p.name(), p.address(), p.active(),
                    p.latitude(), p.longitude(),
                    p.hasParking(), p.hasFittingRoom(),
                    p.hasCardPayment(), p.wheelchairAccessible(),
                    p.timezone(), p.temporarilyClosed(),
                    PickpointAdapter.computeIsOpenNow(p, hours));
        }
    }
}
