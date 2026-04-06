package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.discount.CreateDiscountUseCase;
import tj.radolfa.application.ports.in.discount.DisableDiscountUseCase;
import tj.radolfa.application.ports.in.discount.UpdateDiscountUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.infrastructure.web.dto.CreateDiscountRequest;
import tj.radolfa.infrastructure.web.dto.DiscountResponse;
import tj.radolfa.infrastructure.web.dto.UpdateDiscountRequest;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/discounts")
public class DiscountController {

    private final LoadDiscountPort loadDiscountPort;
    private final CreateDiscountUseCase createDiscountUseCase;
    private final UpdateDiscountUseCase updateDiscountUseCase;
    private final DisableDiscountUseCase disableDiscountUseCase;

    public DiscountController(LoadDiscountPort loadDiscountPort,
                              CreateDiscountUseCase createDiscountUseCase,
                              UpdateDiscountUseCase updateDiscountUseCase,
                              DisableDiscountUseCase disableDiscountUseCase) {
        this.loadDiscountPort = loadDiscountPort;
        this.createDiscountUseCase = createDiscountUseCase;
        this.updateDiscountUseCase = updateDiscountUseCase;
        this.disableDiscountUseCase = disableDiscountUseCase;
    }

    @GetMapping
    public Page<DiscountResponse> list(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        DiscountFilter filter = new DiscountFilter(typeId, status, from, to);
        return loadDiscountPort.findAll(filter, pageable).map(DiscountResponse::fromDomain);
    }

    @GetMapping("/{id}")
    public DiscountResponse getById(@PathVariable Long id) {
        return loadDiscountPort.findById(id)
                .map(DiscountResponse::fromDomain)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(@Valid @RequestBody CreateDiscountRequest request) {
        return DiscountResponse.fromDomain(
                createDiscountUseCase.execute(new CreateDiscountUseCase.Command(
                        request.typeId(),
                        request.itemCodes(),
                        request.discountValue(),
                        request.validFrom(),
                        request.validUpto(),
                        request.title(),
                        request.colorHex()
                )));
    }

    @PutMapping("/{id}")
    public DiscountResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateDiscountRequest request) {
        return DiscountResponse.fromDomain(
                updateDiscountUseCase.execute(new UpdateDiscountUseCase.Command(
                        id,
                        request.typeId(),
                        request.itemCodes(),
                        request.discountValue(),
                        request.validFrom(),
                        request.validUpto(),
                        request.title(),
                        request.colorHex()
                )));
    }

    @PatchMapping("/{id}/disable")
    public DiscountResponse disable(@PathVariable Long id) {
        return DiscountResponse.fromDomain(
                disableDiscountUseCase.execute(new DisableDiscountUseCase.Command(id, true)));
    }

    @PatchMapping("/{id}/enable")
    public DiscountResponse enable(@PathVariable Long id) {
        return DiscountResponse.fromDomain(
                disableDiscountUseCase.execute(new DisableDiscountUseCase.Command(id, false)));
    }
}
