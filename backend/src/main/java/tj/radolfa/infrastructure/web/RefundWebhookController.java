package tj.radolfa.infrastructure.web;

// When implementing: load CustomerReturn by gatewayRefundId from the request body,
// call customerReturn.markRefunded(), save, and confirm idempotency
// (ignore duplicate webhook calls if already REFUNDED).

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/refund")
@Tag(name = "Webhooks — Refund", description = "Payment gateway refund confirmation webhook")
public class RefundWebhookController {

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmRefund(@RequestBody Map<String, Object> body) {
        // TODO: implement when gateway is decided.
        return ResponseEntity.ok().build();
    }
}
