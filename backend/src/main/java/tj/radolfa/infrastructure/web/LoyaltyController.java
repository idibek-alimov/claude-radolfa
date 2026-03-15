package tj.radolfa.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.GetLoyaltyTiersUseCase;
import tj.radolfa.application.ports.in.UpdateLoyaltyTierUseCase;
import tj.radolfa.application.ports.in.UpdateLoyaltyTierUseCase.UpdateTierColorCommand;
import tj.radolfa.infrastructure.web.dto.LoyaltyTierDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loyalty-tiers")
public class LoyaltyController {

    private final GetLoyaltyTiersUseCase getLoyaltyTiersUseCase;
    private final UpdateLoyaltyTierUseCase updateLoyaltyTierUseCase;

    public LoyaltyController(GetLoyaltyTiersUseCase getLoyaltyTiersUseCase,
                              UpdateLoyaltyTierUseCase updateLoyaltyTierUseCase) {
        this.getLoyaltyTiersUseCase = getLoyaltyTiersUseCase;
        this.updateLoyaltyTierUseCase = updateLoyaltyTierUseCase;
    }

    @GetMapping
    public ResponseEntity<List<LoyaltyTierDto>> getAllTiers() {
        var tiers = getLoyaltyTiersUseCase.findAll()
                .stream()
                .map(LoyaltyTierDto::fromDomain)
                .toList();
        return ResponseEntity.ok(tiers);
    }

    record UpdateColorRequest(String color) {}

    @PatchMapping("/{id}/color")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM')")
    public ResponseEntity<Void> updateTierColor(@PathVariable Long id,
                                                @RequestBody UpdateColorRequest request) {
        updateLoyaltyTierUseCase.updateColor(new UpdateTierColorCommand(id, request.color()));
        return ResponseEntity.noContent().build();
    }
}
