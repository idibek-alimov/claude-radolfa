package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.discount.type.CreateDiscountTypeUseCase;
import tj.radolfa.application.ports.in.discount.type.DeleteDiscountTypeUseCase;
import tj.radolfa.application.ports.in.discount.type.UpdateDiscountTypeUseCase;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.domain.exception.DiscountTypeInUseException;
import tj.radolfa.infrastructure.web.dto.CreateDiscountTypeRequest;
import tj.radolfa.infrastructure.web.dto.DiscountTypeResponse;
import tj.radolfa.infrastructure.web.dto.UpdateDiscountTypeRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/discount-types")
public class DiscountTypeController {

    private final LoadDiscountTypePort loadDiscountTypePort;
    private final CreateDiscountTypeUseCase createDiscountTypeUseCase;
    private final UpdateDiscountTypeUseCase updateDiscountTypeUseCase;
    private final DeleteDiscountTypeUseCase deleteDiscountTypeUseCase;

    public DiscountTypeController(LoadDiscountTypePort loadDiscountTypePort,
                                  CreateDiscountTypeUseCase createDiscountTypeUseCase,
                                  UpdateDiscountTypeUseCase updateDiscountTypeUseCase,
                                  DeleteDiscountTypeUseCase deleteDiscountTypeUseCase) {
        this.loadDiscountTypePort = loadDiscountTypePort;
        this.createDiscountTypeUseCase = createDiscountTypeUseCase;
        this.updateDiscountTypeUseCase = updateDiscountTypeUseCase;
        this.deleteDiscountTypeUseCase = deleteDiscountTypeUseCase;
    }

    @GetMapping
    public List<DiscountTypeResponse> list() {
        return loadDiscountTypePort.findAll().stream()
                .map(DiscountTypeResponse::fromDomain)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountTypeResponse create(@Valid @RequestBody CreateDiscountTypeRequest request) {
        return DiscountTypeResponse.fromDomain(
                createDiscountTypeUseCase.execute(
                        new CreateDiscountTypeUseCase.Command(request.name(), request.rank(), request.stackingPolicy())));
    }

    @PutMapping("/{id}")
    public DiscountTypeResponse update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateDiscountTypeRequest request) {
        return DiscountTypeResponse.fromDomain(
                updateDiscountTypeUseCase.execute(
                        new UpdateDiscountTypeUseCase.Command(id, request.name(), request.rank(), request.stackingPolicy())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            deleteDiscountTypeUseCase.execute(id);
            return ResponseEntity.noContent().build();
        } catch (DiscountTypeInUseException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Discount type is still in use",
                            "discountCount", e.getDiscountCount()
                    ));
        }
    }
}
