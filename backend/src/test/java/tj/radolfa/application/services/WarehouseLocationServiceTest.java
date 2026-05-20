package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.application.ports.out.SaveWarehouseLocationPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseLocationServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadWarehouseLocationPort implements LoadWarehouseLocationPort {
        final WarehouseZone  zone;
        final WarehouseShelf shelf;

        FakeLoadWarehouseLocationPort(WarehouseZone zone, WarehouseShelf shelf) {
            this.zone  = zone;
            this.shelf = shelf;
        }

        @Override public List<WarehouseZone>      findAllZones()               { return zone != null ? List.of(zone) : List.of(); }
        @Override public Optional<WarehouseZone>  findZoneById(Long id)        { return zone != null && zone.id().equals(id) ? Optional.of(zone) : Optional.empty(); }
        @Override public List<WarehouseShelf>     findShelvesByZoneId(Long id) { return List.of(); }
        @Override public Optional<WarehouseShelf> findShelfById(Long id)       { return shelf != null && shelf.id().equals(id) ? Optional.of(shelf) : Optional.empty(); }
        @Override public List<WarehouseBin>       findBinsByShelfId(Long id)   { return List.of(); }
        @Override public Optional<WarehouseBin>   findBinById(Long id)         { return Optional.empty(); }
    }

    static class FakeSaveWarehouseLocationPort implements SaveWarehouseLocationPort {
        final List<WarehouseZone>  savedZones  = new ArrayList<>();
        final List<WarehouseShelf> savedShelves = new ArrayList<>();
        final List<WarehouseBin>   savedBins   = new ArrayList<>();
        final List<Long>           deletedZoneIds = new ArrayList<>();

        @Override public WarehouseZone saveZone(WarehouseZone z) {
            var saved = new WarehouseZone(10L, z.code(), z.label());
            savedZones.add(saved); return saved;
        }
        @Override public WarehouseShelf saveShelf(WarehouseShelf s) {
            var saved = new WarehouseShelf(20L, s.zoneId(), s.code(), s.label());
            savedShelves.add(saved); return saved;
        }
        @Override public WarehouseBin saveBin(WarehouseBin b) {
            var saved = new WarehouseBin(30L, b.shelfId(), b.code());
            savedBins.add(saved); return saved;
        }
        @Override public void deleteZone(Long id)  { deletedZoneIds.add(id); }
        @Override public void deleteShelf(Long id) {}
        @Override public void deleteBin(Long id)   {}
    }

    static WarehouseLocationService service(FakeLoadWarehouseLocationPort load,
                                             FakeSaveWarehouseLocationPort save) {
        return new WarehouseLocationService(load, save);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createZone → port.saveZone called with given code and label")
    void createZone_savesCorrectly() {
        var save = new FakeSaveWarehouseLocationPort();
        var zone = service(new FakeLoadWarehouseLocationPort(null, null), save)
                .createZone("A", "Zone A");

        assertEquals(1, save.savedZones.size());
        assertEquals("A", zone.code());
        assertEquals("Zone A", zone.label());
        assertNotNull(zone.id());
    }

    @Test
    @DisplayName("createShelf with non-existent zone → ResourceNotFoundException")
    void createShelf_unknownZone_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(new FakeLoadWarehouseLocationPort(null, null),
                        new FakeSaveWarehouseLocationPort())
                        .createShelf(99L, "3", "Row 3"));
    }

    @Test
    @DisplayName("createShelf with existing zone → port.saveShelf called; shelf has correct zoneId")
    void createShelf_existingZone_savesCorrectly() {
        var existingZone = new WarehouseZone(1L, "A", "Zone A");
        var save = new FakeSaveWarehouseLocationPort();
        var shelf = service(new FakeLoadWarehouseLocationPort(existingZone, null), save)
                .createShelf(1L, "3", "Row 3");

        assertEquals(1, save.savedShelves.size());
        assertEquals(1L, shelf.zoneId());
        assertEquals("3", shelf.code());
    }

    @Test
    @DisplayName("createBin with non-existent shelf → ResourceNotFoundException")
    void createBin_unknownShelf_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(new FakeLoadWarehouseLocationPort(null, null),
                        new FakeSaveWarehouseLocationPort())
                        .createBin(99L, "7"));
    }

    @Test
    @DisplayName("createBin with existing shelf → port.saveBin called; bin has correct shelfId")
    void createBin_existingShelf_savesCorrectly() {
        var existingShelf = new WarehouseShelf(2L, 1L, "3", "Row 3");
        var save = new FakeSaveWarehouseLocationPort();
        var bin = service(new FakeLoadWarehouseLocationPort(null, existingShelf), save)
                .createBin(2L, "7");

        assertEquals(1, save.savedBins.size());
        assertEquals(2L, bin.shelfId());
        assertEquals("7", bin.code());
    }

    @Test
    @DisplayName("deleteZone → port.deleteZone invoked with correct id")
    void deleteZone_invokesPort() {
        var save = new FakeSaveWarehouseLocationPort();
        service(new FakeLoadWarehouseLocationPort(null, null), save).deleteZone(5L);

        assertEquals(List.of(5L), save.deletedZoneIds);
    }
}
