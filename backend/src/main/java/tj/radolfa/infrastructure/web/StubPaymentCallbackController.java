package tj.radolfa.infrastructure.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.in.payment.ConfirmPaymentUseCase;

import java.net.URI;

/**
 * Dev/test only — simulates the payment provider's success callback.
 *
 * Real flow:  provider redirects user → provider also POSTs webhook → backend confirms.
 * Stub flow:  stub redirectUrl → this GET → confirm payment → redirect to /payment/return.
 *
 * Next.js proxies /api/* to the backend, so the browser follows this naturally.
 */
@RestController
@RequestMapping("/api/v1/payments/stub")
@Profile("dev | test")
public class StubPaymentCallbackController {

    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    public StubPaymentCallbackController(ConfirmPaymentUseCase confirmPaymentUseCase) {
        this.confirmPaymentUseCase = confirmPaymentUseCase;
    }

    @GetMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestParam String tx,
            @RequestParam Long orderId) {

        confirmPaymentUseCase.execute(tx);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/payment/return?orderId=" + orderId))
                .build();
    }
}
