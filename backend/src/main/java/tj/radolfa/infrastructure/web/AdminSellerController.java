package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.seller.CreateSellerUseCase;
import tj.radolfa.application.ports.out.LoadSellerPort;
import tj.radolfa.infrastructure.web.dto.CreateSellerRequestDto;
import tj.radolfa.infrastructure.web.dto.SellerDto;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/sellers")
public class AdminSellerController {

    private final CreateSellerUseCase createSellerUseCase;
    private final LoadSellerPort loadSellerPort;

    public AdminSellerController(CreateSellerUseCase createSellerUseCase,
                                 LoadSellerPort loadSellerPort) {
        this.createSellerUseCase = createSellerUseCase;
        this.loadSellerPort = loadSellerPort;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerDto> createSeller(@RequestBody @Valid CreateSellerRequestDto request) {
        var seller = createSellerUseCase.execute(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/v1/admin/sellers/" + seller.id()))
                .body(SellerDto.from(seller));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<SellerDto>> listSellers(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "")   String search) {
        var result = loadSellerPort.findAllPaged(page, size, search);
        var dto = result.map(SellerDto::from);
        return ResponseEntity.ok(PageResponse.from(dto));
    }
}
