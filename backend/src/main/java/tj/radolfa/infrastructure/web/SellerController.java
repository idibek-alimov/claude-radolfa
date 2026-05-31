package tj.radolfa.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.seller.GetMySellerProfileUseCase;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.SellerDto;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerController {

    private final GetMySellerProfileUseCase getMySellerProfileUseCase;

    public SellerController(GetMySellerProfileUseCase getMySellerProfileUseCase) {
        this.getMySellerProfileUseCase = getMySellerProfileUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerDto> getMyProfile(
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        var seller = getMySellerProfileUseCase.execute(principal.userId());
        return ResponseEntity.ok(SellerDto.from(seller));
    }
}
