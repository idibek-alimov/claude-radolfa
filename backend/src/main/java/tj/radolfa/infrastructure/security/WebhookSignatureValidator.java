package tj.radolfa.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates HMAC-SHA256 signatures on incoming payment webhook payloads.
 *
 * <p>Expected header: {@code X-Webhook-Signature: <hex-encoded HMAC-SHA256>}
 *
 * <p>Signature validation is skipped entirely when
 * {@link WebhookProperties#isValidationEnabled()} returns {@code false}
 * (i.e. {@code WEBHOOK_SECRET} is not set — local dev only).
 */
@Component
public class WebhookSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    private static final String ALGORITHM = "HmacSHA256";

    private final WebhookProperties webhookProperties;

    public WebhookSignatureValidator(WebhookProperties webhookProperties) {
        this.webhookProperties = webhookProperties;
    }

    /**
     * Returns {@code true} if the signature is valid (or validation is disabled).
     * Returns {@code false} if the signature is missing, malformed, or does not match.
     *
     * @param rawPayload         the raw request body bytes
     * @param signatureHeader    value of the {@code X-Webhook-Signature} header (may be null)
     */
    public boolean isValid(byte[] rawPayload, String signatureHeader) {
        if (!webhookProperties.isValidationEnabled()) {
            log.debug("[WEBHOOK] Signature validation disabled (no WEBHOOK_SECRET configured)");
            return true;
        }

        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("[WEBHOOK] Rejected: missing X-Webhook-Signature header");
            return false;
        }

        try {
            String expected = computeHmac(rawPayload, webhookProperties.secret());
            boolean match = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.strip().getBytes(StandardCharsets.UTF_8));

            if (!match) {
                log.warn("[WEBHOOK] Rejected: signature mismatch");
            }
            return match;
        } catch (Exception e) {
            log.error("[WEBHOOK] Signature computation failed: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmac(byte[] payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        return HexFormat.of().formatHex(mac.doFinal(payload));
    }
}
