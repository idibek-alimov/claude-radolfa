package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.AssignSkuToBinPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AssignSkuToBinServiceTest {

    static final Long SKU_ID = 1L;
    static final Long BIN_ID = 10L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeAssignSkuToBinPort implements AssignSkuToBinPort {
        Long capturedSkuId;
        Long capturedBinId;
        boolean called = false;

        @Override public void assign(Long skuId, Long binId) {
            this.capturedSkuId = skuId;
            this.capturedBinId = binId;
            this.called        = true;
        }
    }

    static class FakeLoadSkuPort implements LoadSkuPort {
        final Sku sku;
        FakeLoadSkuPort(Sku sku) { this.sku = sku; }

        @Override public Optional<Sku>  findSkuById(Long id)          { return sku != null && sku.getId().equals(id) ? Optional.of(sku) : Optional.empty(); }
        @Override public Optional<Sku>  findBySkuCode(String code)     { return Optional.empty(); }
        @Override public List<Sku>      findSkusByVariantId(Long id)   { return List.of(); }
        @Override public List<Sku>      findAllByIds(Collection<Long> ids) { return List.of(); }
    }

    static class FakeLoadWarehouseLocationPort implements LoadWarehouseLocationPort {
        final WarehouseBin bin;
        FakeLoadWarehouseLocationPort(WarehouseBin bin) { this.bin = bin; }

        @Override public List<WarehouseZone>      findAllZones()               { return List.of(); }
        @Override public Optional<WarehouseZone>  findZoneById(Long id)        { return Optional.empty(); }
        @Override public List<WarehouseShelf>     findShelvesByZoneId(Long id) { return List.of(); }
        @Override public Optional<WarehouseShelf> findShelfById(Long id)       { return Optional.empty(); }
        @Override public List<WarehouseBin>       findBinsByShelfId(Long id)   { return List.of(); }
        @Override public Optional<WarehouseBin>   findBinById(Long id)         {
            return bin != null && bin.id().equals(id) ? Optional.of(bin) : Optional.empty();
        }
    }

    static Sku sku() {
        return new Sku(SKU_ID, 10L, "SKU-001", "M", 5, new Money(BigDecimal.TEN));
    }

    static WarehouseBin bin() {
        return new WarehouseBin(BIN_ID, 2L, "7");
    }

    static AssignSkuToBinService service(FakeLoadSkuPort skuPort,
                                          FakeLoadWarehouseLocationPort locationPort,
                                          FakeAssignSkuToBinPort assignPort) {
        return new AssignSkuToBinService(assignPort, skuPort, locationPort);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid SKU + valid bin → port.assign called with correct ids")
    void validSkuAndBin_assignsCalled() {
        var assignPort = new FakeAssignSkuToBinPort();
        service(new FakeLoadSkuPort(sku()), new FakeLoadWarehouseLocationPort(bin()), assignPort)
                .execute(SKU_ID, BIN_ID);

        assertTrue(assignPort.called);
        assertEquals(SKU_ID, assignPort.capturedSkuId);
        assertEquals(BIN_ID, assignPort.capturedBinId);
    }

    @Test
    @DisplayName("Unknown SKU → ResourceNotFoundException; port.assign not called")
    void unknownSku_throws() {
        var assignPort = new FakeAssignSkuToBinPort();
        assertThrows(ResourceNotFoundException.class,
                () -> service(new FakeLoadSkuPort(null), new FakeLoadWarehouseLocationPort(bin()), assignPort)
                        .execute(SKU_ID, BIN_ID));

        assertFalse(assignPort.called);
    }

    @Test
    @DisplayName("Valid SKU + unknown bin → ResourceNotFoundException")
    void unknownBin_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(new FakeLoadSkuPort(sku()), new FakeLoadWarehouseLocationPort(null),
                        new FakeAssignSkuToBinPort()).execute(SKU_ID, BIN_ID));
    }

    @Test
    @DisplayName("binId = null (unassign) → port.assign called with null; no bin existence check")
    void nullBin_unassigns() {
        var assignPort = new FakeAssignSkuToBinPort();
        // FakeLoadWarehouseLocationPort has no bin — but null binId skips the check
        service(new FakeLoadSkuPort(sku()), new FakeLoadWarehouseLocationPort(null), assignPort)
                .execute(SKU_ID, null);

        assertTrue(assignPort.called);
        assertNull(assignPort.capturedBinId);
    }
}
