package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.payment.ConfirmPaymentUseCase;
import tj.radolfa.infrastructure.security.WebhookSignatureValidator;

import java.nio.charset.StandardCharsets;

/**
 * Receives payment provider callbacks (webhooks).
 *
 * <p>No JWT auth — the endpoint is public. Signature validation is performed
 * here using HMAC-SHA256 before the payload is handed to the application layer.
 * Requests with a missing or invalid {@code X-Webhook-Signature} header are
 * rejected with {@code 401 Unauthorized}.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Payment provider callback endpoints")
public class PaymentWebhookController {

    private final ConfirmPaymentUseCase        confirmPaymentUseCase;
    private final WebhookSignatureValidator    signatureValidator;

    public PaymentWebhookController(ConfirmPaymentUseCase confirmPaymentUseCase,
                                    WebhookSignatureValidator signatureValidator) {
        this.confirmPaymentUseCase = confirmPaymentUseCase;
        this.signatureValidator    = signatureValidator;
    }

    @PostMapping("/payment")
    @Operation(summary = "Payment provider webhook — confirms a completed payment")
    public ResponseEntity<Void> handlePaymentCallback(
            @RequestBody String rawPayload,
            @RequestParam(required = false) String transactionId,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {

        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] payloadBytes = rawPayload.getBytes(StandardCharsets.UTF_8);
        if (!signatureValidator.isValid(payloadBytes, signature)) {
            return ResponseEntity.status(401).build();
        }

        confirmPaymentUseCase.execute(transactionId);
        return ResponseEntity.ok().build();
    }
}
