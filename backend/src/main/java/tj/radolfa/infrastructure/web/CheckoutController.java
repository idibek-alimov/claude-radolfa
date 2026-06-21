package tj.radolfa.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.infrastructure.config.CheckoutProperties;
import tj.radolfa.infrastructure.web.dto.CheckoutOptionsResponseDto;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutProperties checkoutProperties;

    public CheckoutController(CheckoutProperties checkoutProperties) {
        this.checkoutProperties = checkoutProperties;
    }

    @GetMapping("/options")
    public ResponseEntity<CheckoutOptionsResponseDto> getOptions() {
        return ResponseEntity.ok(CheckoutOptionsResponseDto.from(checkoutProperties));
    }
}
