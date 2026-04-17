package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.discount.*;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.DiscountedProductFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/discounts")
public class DiscountController {

    private final LoadDiscountPort loadDiscountPort;
    private final CreateDiscountUseCase createDiscountUseCase;
    private final UpdateDiscountUseCase updateDiscountUseCase;
    private final DisableDiscountUseCase disableDiscountUseCase;
    private final ListDiscountedProductsUseCase listDiscountedProductsUseCase;
    private final FindDiscountOverlapsUseCase findDiscountOverlapsUseCase;
    private final BulkToggleDiscountUseCase bulkToggleDiscountUseCase;
    private final BulkDeleteDiscountUseCase bulkDeleteDiscountUseCase;
    private final BulkDuplicateDiscountUseCase bulkDuplicateDiscountUseCase;

    public DiscountController(LoadDiscountPort loadDiscountPort,
                              CreateDiscountUseCase createDiscountUseCase,
                              UpdateDiscountUseCase updateDiscountUseCase,
                              DisableDiscountUseCase disableDiscountUseCase,
                              ListDiscountedProductsUseCase listDiscountedProductsUseCase,
                              FindDiscountOverlapsUseCase findDiscountOverlapsUseCase,
                              BulkToggleDiscountUseCase bulkToggleDiscountUseCase,
                              BulkDeleteDiscountUseCase bulkDeleteDiscountUseCase,
                              BulkDuplicateDiscountUseCase bulkDuplicateDiscountUseCase) {
        this.loadDiscountPort = loadDiscountPort;
        this.createDiscountUseCase = createDiscountUseCase;
        this.updateDiscountUseCase = updateDiscountUseCase;
        this.disableDiscountUseCase = disableDiscountUseCase;
        this.listDiscountedProductsUseCase = listDiscountedProductsUseCase;
        this.findDiscountOverlapsUseCase = findDiscountOverlapsUseCase;
        this.bulkToggleDiscountUseCase = bulkToggleDiscountUseCase;
        this.bulkDeleteDiscountUseCase = bulkDeleteDiscountUseCase;
        this.bulkDuplicateDiscountUseCase = bulkDuplicateDiscountUseCase;
    }

    private static final Set<String> ALLOWED_SORT = Set.of(
            "id", "title", "discountValue", "validFrom", "validUpto");

    private Pageable sanitize(Pageable pageable) {
        Sort filtered = Sort.by(pageable.getSort().stream()
                .filter(o -> ALLOWED_SORT.contains(o.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                filtered.isEmpty() ? Sort.by("id") : filtered);
    }

    // ---- Campaign list ----

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<DiscountResponse> list(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        DiscountFilter filter = new DiscountFilter(typeId, status, from, to, search);
        return loadDiscountPort.findAll(filter, sanitize(pageable)).map(DiscountResponse::fromDomain);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public DiscountResponse getById(@PathVariable Long id) {
        return loadDiscountPort.findById(id)
                .map(DiscountResponse::fromDomain)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public DiscountResponse create(@Valid @RequestBody CreateDiscountRequest request) {
        return DiscountResponse.fromDomain(
                createDiscountUseCase.execute(new CreateDiscountUseCase.Command(
                        request.typeId(), request.itemCodes(), request.discountValue(),
                        request.validFrom(), request.validUpto(), request.title(), request.colorHex()
                )));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public DiscountResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateDiscountRequest request) {
        return DiscountResponse.fromDomain(
                updateDiscountUseCase.execute(new UpdateDiscountUseCase.Command(
                        id, request.typeId(), request.itemCodes(), request.discountValue(),
                        request.validFrom(), request.validUpto(), request.title(), request.colorHex()
                )));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public DiscountResponse disable(@PathVariable Long id) {
        return DiscountResponse.fromDomain(
                disableDiscountUseCase.execute(new DisableDiscountUseCase.Command(id, true)));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public DiscountResponse enable(@PathVariable Long id) {
        return DiscountResponse.fromDomain(
                disableDiscountUseCase.execute(new DisableDiscountUseCase.Command(id, false)));
    }

    // ---- Discounted products view ----

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<DiscountedProductRowResponse> listDiscountedProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) BigDecimal minDeltaPercent,
            @RequestParam(required = false) BigDecimal maxDeltaPercent,
            @PageableDefault(size = 20, sort = "skuCode") Pageable pageable) {

        DiscountedProductFilter filter = new DiscountedProductFilter(search, campaignId, minDeltaPercent, maxDeltaPercent);
        return listDiscountedProductsUseCase.execute(filter, pageable)
                .map(DiscountedProductRowResponse::fromDomain);
    }

    @GetMapping("/{id}/skus")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<DiscountedProductRowResponse> getCampaignSkus(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "skuCode") Pageable pageable) {

        DiscountedProductFilter filter = new DiscountedProductFilter(search, id, null, null);
        return listDiscountedProductsUseCase.execute(filter, pageable)
                .map(DiscountedProductRowResponse::fromDomain);
    }

    // ---- Overlap detection ----

    @GetMapping("/overlaps")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<DiscountOverlapRowResponse> getOverlaps() {
        return findDiscountOverlapsUseCase.execute().stream()
                .map(DiscountOverlapRowResponse::fromDomain)
                .toList();
    }

    // ---- Bulk operations ----

    record BulkIdsRequest(@NotEmpty List<Long> ids) {}

    @PatchMapping("/bulk/enable")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Map<String, Integer> bulkEnable(@Valid @RequestBody BulkIdsRequest request) {
        int affected = bulkToggleDiscountUseCase.execute(
                new BulkToggleDiscountUseCase.Command(request.ids(), false));
        return Map.of("affected", affected);
    }

    @PatchMapping("/bulk/disable")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Map<String, Integer> bulkDisable(@Valid @RequestBody BulkIdsRequest request) {
        int affected = bulkToggleDiscountUseCase.execute(
                new BulkToggleDiscountUseCase.Command(request.ids(), true));
        return Map.of("affected", affected);
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Map<String, Integer> bulkDelete(@Valid @RequestBody BulkIdsRequest request) {
        int affected = bulkDeleteDiscountUseCase.execute(
                new BulkDeleteDiscountUseCase.Command(request.ids()));
        return Map.of("affected", affected);
    }

    @PostMapping("/bulk/duplicate")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<DiscountResponse> bulkDuplicate(@Valid @RequestBody BulkIdsRequest request) {
        return bulkDuplicateDiscountUseCase.execute(
                new BulkDuplicateDiscountUseCase.Command(request.ids())).stream()
                .map(DiscountResponse::fromDomain)
                .toList();
    }
}
