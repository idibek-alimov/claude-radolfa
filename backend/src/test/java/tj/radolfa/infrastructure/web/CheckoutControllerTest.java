package tj.radolfa.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.infrastructure.config.CheckoutProperties;
import tj.radolfa.infrastructure.web.dto.CheckoutOptionsResponseDto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckoutControllerTest {

    @Test
    @DisplayName("GET /options returns the configured COD handling fee and both payment methods")
    void getOptions_returnsConfiguredFeeAndPaymentMethods() {
        CheckoutController controller = new CheckoutController(new CheckoutProperties(new BigDecimal("15")));

        CheckoutOptionsResponseDto body = controller.getOptions().getBody();

        assertEquals(new BigDecimal("15"), body.codHandlingFee());
        assertEquals(java.util.List.of("CARD", "COD"), body.paymentMethods());
    }
}
