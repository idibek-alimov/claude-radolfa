package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.home.ManageHomeBannerUseCase;
import tj.radolfa.application.ports.out.LoadHomeBannerPort;
import tj.radolfa.infrastructure.web.dto.HomeBannerDto;
import tj.radolfa.infrastructure.web.dto.HomeBannerRequestDto;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/home-banners")
@Tag(name = "Admin — Home Banners", description = "MANAGER+ADMIN endpoints for managing homepage hero banners")
public class AdminHomeBannerController {

    private final ManageHomeBannerUseCase manageHomeBannerUseCase;
    private final LoadHomeBannerPort loadHomeBannerPort;

    public AdminHomeBannerController(ManageHomeBannerUseCase manageHomeBannerUseCase,
                                     LoadHomeBannerPort loadHomeBannerPort) {
        this.manageHomeBannerUseCase = manageHomeBannerUseCase;
        this.loadHomeBannerPort = loadHomeBannerPort;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "List all home banners", description = "Returns all banners (active and inactive). MANAGER + ADMIN.")
    public ResponseEntity<List<HomeBannerDto>> list() {
        List<HomeBannerDto> dtos = loadHomeBannerPort.findAll()
                .stream().map(HomeBannerDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create home banner", description = "Creates a new homepage hero banner. MANAGER + ADMIN.")
    public ResponseEntity<HomeBannerDto> create(@RequestBody @Valid HomeBannerRequestDto request) {
        var banner = manageHomeBannerUseCase.create(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/v1/admin/home-banners/" + banner.id()))
                .body(HomeBannerDto.from(banner));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update home banner", description = "Replaces a banner by ID. MANAGER + ADMIN.")
    public ResponseEntity<HomeBannerDto> update(@PathVariable Long id,
                                                @RequestBody @Valid HomeBannerRequestDto request) {
        var banner = manageHomeBannerUseCase.update(id, request.toCommand());
        return ResponseEntity.ok(HomeBannerDto.from(banner));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Delete home banner", description = "Deletes a banner by ID. MANAGER + ADMIN.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        manageHomeBannerUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
