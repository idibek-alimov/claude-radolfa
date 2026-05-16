package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.UpdateSkuDimensionsUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateSkuDimensionsServiceTest {

    private static final Long SKU_ID     = 1L;
    private static final Long VARIANT_ID = 10L;

    private FakeLoadSkuPort         loadSkuPort;
    private CapturingSavePort       savePort;
    private UpdateSkuDimensionsService service;

    @BeforeEach
    void setUp() {
        Sku sku = new Sku(SKU_ID, VARIANT_ID, "SKU-001", "M", 10,
                new Money(new BigDecimal("29.99")));
        loadSkuPort = new FakeLoadSkuPort(sku);
        savePort    = new CapturingSavePort();
        service = new UpdateSkuDimensionsService(loadSkuPort, savePort);
    }

    @Test
    void execute_allFields_persistedOnSku() {
        service.execute(new UpdateSkuDimensionsUseCase.Command(
                SKU_ID, new BigDecimal("0.350"), 20, 15, 5));

        Sku saved = savePort.lastSaved;
        assertNotNull(saved);
        assertEquals(new BigDecimal("0.350"), saved.getWeightKg());
        assertEquals(20, saved.getLengthCm());
        assertEquals(15, saved.getWidthCm());
        assertEquals(5,  saved.getHeightCm());
    }

    @Test
    void execute_nullFields_clearExistingValues() {
        // set initial values via first call
        service.execute(new UpdateSkuDimensionsUseCase.Command(SKU_ID, new BigDecimal("1.0"), 30, 20, 10));

        // clear them with nulls
        service.execute(new UpdateSkuDimensionsUseCase.Command(SKU_ID, null, null, null, null));

        Sku saved = savePort.lastSaved;
        assertNull(saved.getWeightKg());
        assertNull(saved.getLengthCm());
        assertNull(saved.getWidthCm());
        assertNull(saved.getHeightCm());
    }

    @Test
    void execute_unknownSku_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new UpdateSkuDimensionsUseCase.Command(
                        999L, new BigDecimal("0.5"), 10, 10, 10)));
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadSkuPort implements LoadSkuPort {
        private final Sku sku;
        FakeLoadSkuPort(Sku sku) { this.sku = sku; }

        @Override public Optional<Sku> findBySkuCode(String code) { return Optional.empty(); }
        @Override public Optional<Sku> findSkuById(Long id) {
            return sku.getId().equals(id) ? Optional.of(sku) : Optional.empty();
        }
        @Override public List<Sku> findSkusByVariantId(Long variantId) { return List.of(); }
        @Override public List<Sku> findAllByIds(Collection<Long> ids) { return List.of(); }
    }

    static class CapturingSavePort implements SaveProductHierarchyPort {
        Sku lastSaved;

        @Override public Sku saveSku(Sku sku, Long variantId) {
            this.lastSaved = sku;
            return sku;
        }
        @Override public tj.radolfa.domain.model.ProductBase saveBase(tj.radolfa.domain.model.ProductBase pb) { return pb; }
        @Override public tj.radolfa.domain.model.ListingVariant saveVariant(tj.radolfa.domain.model.ListingVariant v, Long productBaseId) { return v; }
    }
}
