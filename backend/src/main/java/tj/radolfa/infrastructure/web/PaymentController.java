package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.payment.InitiatePaymentUseCase;
import tj.radolfa.application.ports.in.payment.RefundPaymentUseCase;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.InitiatePaymentResponseDto;
import tj.radolfa.infrastructure.web.dto.PaymentStatusDto;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment endpoints")
public class PaymentController {

    private final InitiatePaymentUseCase initiatePaymentUseCase;
    private final RefundPaymentUseCase   refundPaymentUseCase;
    private final LoadPaymentPort        loadPaymentPort;

    public PaymentController(InitiatePaymentUseCase initiatePaymentUseCase,
                             RefundPaymentUseCase refundPaymentUseCase,
                             LoadPaymentPort loadPaymentPort) {
        this.initiatePaymentUseCase = initiatePaymentUseCase;
        this.refundPaymentUseCase   = refundPaymentUseCase;
        this.loadPaymentPort        = loadPaymentPort;
    }

    @PostMapping("/initiate/{orderId}")
    @Operation(summary = "Initiate payment for a PENDING order (USER, must own the order)")
    public ResponseEntity<InitiatePaymentResponseDto> initiate(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "STUB") String provider,
            @AuthenticationPrincipal JwtAuthenticatedUser user) {

        InitiatePaymentUseCase.Result result =
                initiatePaymentUseCase.execute(orderId, user.userId(), provider);

        return ResponseEntity.ok(new InitiatePaymentResponseDto(result.paymentId(), result.redirectUrl()));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get payment status for an order (USER)")
    public ResponseEntity<PaymentStatusDto> getStatus(@PathVariable Long orderId) {
        return loadPaymentPort.findByOrderId(orderId)
                .map(p -> ResponseEntity.ok(PaymentStatusDto.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{orderId}/refund")
    @Operation(summary = "Refund a completed payment (ADMIN only)")
    public ResponseEntity<Void> refund(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser user) {

        refundPaymentUseCase.execute(orderId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
