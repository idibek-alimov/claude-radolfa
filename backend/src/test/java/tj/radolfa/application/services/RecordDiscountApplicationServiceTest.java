package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.domain.model.DiscountApplication;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordDiscountApplicationServiceTest {

    // ---- Fake ----

    static class FakeSaveDiscountApplicationPort implements SaveDiscountApplicationPort {
        private final List<DiscountApplication> stored = new ArrayList<>();

        @Override
        public DiscountApplication save(DiscountApplication application) {
            stored.add(application);
            return application;
        }

        List<DiscountApplication> stored() {
            return stored;
        }
    }

    // ---- Setup ----

    private FakeSaveDiscountApplicationPort fakePort;
    private RecordDiscountApplicationService service;

    @BeforeEach
    void setUp() {
        fakePort = new FakeSaveDiscountApplicationPort();
        service  = new RecordDiscountApplicationService(fakePort);
    }

    // ---- Tests ----

    @Test
    @DisplayName("execute: valid command saves application with computed delta and timestamp")
    void execute_validCommand_savesApplicationWithComputedDeltaAndTimestamp() {
        BigDecimal original = new BigDecimal("100.00");
        BigDecimal applied  = new BigDecimal("80.00");
        int qty = 1;

        service.execute(new RecordDiscountApplicationUseCase.Command(
                1L, 2L, 3L, "SKU-001", qty, original, applied));

        assertEquals(1, fakePort.stored().size());
        DiscountApplication app = fakePort.stored().get(0);
        assertEquals(1L, app.discountId());
        assertEquals(2L, app.orderId());
        assertEquals(3L, app.orderLineId());
        assertEquals("SKU-001", app.skuItemCode());
        assertEquals(qty, app.quantity());
        assertEquals(original, app.originalUnitPrice());
        assertEquals(applied, app.appliedUnitPrice());
        assertEquals(new BigDecimal("20.00"), app.discountAmount());
        assertNotNull(app.appliedAt());
    }

    @Test
    @DisplayName("execute: quantity > 1 multiplies per-unit delta into total discount amount")
    void execute_quantityGreaterThanOne_multipliesDelta() {
        BigDecimal original = new BigDecimal("50.00");
        BigDecimal applied  = new BigDecimal("40.00");
        int qty = 3;

        service.execute(new RecordDiscountApplicationUseCase.Command(
                10L, 20L, 30L, "SKU-002", qty, original, applied));

        DiscountApplication app = fakePort.stored().get(0);
        assertEquals(new BigDecimal("30.00"), app.discountAmount()); // (50-40)*3
    }

    @Test
    @DisplayName("execute: zero delta (original == applied) still saves a row with discountAmount=0.00")
    void execute_zeroDelta_stillSaves() {
        BigDecimal price = new BigDecimal("75.00");

        service.execute(new RecordDiscountApplicationUseCase.Command(
                5L, 6L, 7L, "SKU-003", 2, price, price));

        assertEquals(1, fakePort.stored().size());
        assertEquals(new BigDecimal("0.00"), fakePort.stored().get(0).discountAmount());
    }
}
