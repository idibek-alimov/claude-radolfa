package tj.radolfa.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.infrastructure.web.dto.LoyaltyTierDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loyalty-tiers")
public class LoyaltyController {

    private final LoadLoyaltyTierPort loadLoyaltyTierPort;

    public LoyaltyController(LoadLoyaltyTierPort loadLoyaltyTierPort) {
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
    }

    @GetMapping
    public ResponseEntity<List<LoyaltyTierDto>> getAllTiers() {
        var tiers = loadLoyaltyTierPort.findAll()
                .stream()
                .map(LoyaltyTierDto::fromDomain)
                .toList();
        return ResponseEntity.ok(tiers);
    }
}
