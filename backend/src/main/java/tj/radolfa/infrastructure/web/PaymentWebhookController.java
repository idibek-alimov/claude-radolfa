package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.payment.ConfirmPaymentUseCase;

import java.util.Map;

/**
 * Receives payment provider callbacks (webhooks).
 *
 * <p>No JWT auth — the endpoint is public. Signature validation is
 * performed inside {@link ConfirmPaymentUseCase} using the raw payload.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Payment provider callback endpoints")
public class PaymentWebhookController {

    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    public PaymentWebhookController(ConfirmPaymentUseCase confirmPaymentUseCase) {
        this.confirmPaymentUseCase = confirmPaymentUseCase;
    }

    @PostMapping("/payment")
    @Operation(summary = "Payment provider webhook — confirms a completed payment")
    public ResponseEntity<Void> handlePaymentCallback(
            @RequestBody String rawPayload,
            @RequestParam(required = false) String transactionId) {

        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        confirmPaymentUseCase.execute(transactionId, rawPayload);
        return ResponseEntity.ok().build();
    }
}
