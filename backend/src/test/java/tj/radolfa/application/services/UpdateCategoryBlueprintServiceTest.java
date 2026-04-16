package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.UpdateCategoryBlueprintUseCase.Command;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort.BlueprintEntry;
import tj.radolfa.application.ports.out.SaveCategoryBlueprintPort;
import tj.radolfa.application.ports.in.UpdateCategoryBlueprintUseCase;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.AttributeType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpdateCategoryBlueprintService} using in-memory fakes.
 */
class UpdateCategoryBlueprintServiceTest {

    private FakeLoadBlueprintPort fakeLoad;
    private FakeSaveBlueprintPort fakeSave;
    private UpdateCategoryBlueprintService service;

    private static final long CAT_ID = 1L;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadBlueprintPort();
        fakeSave = new FakeSaveBlueprintPort();
        service  = new UpdateCategoryBlueprintService(fakeLoad, fakeSave);
    }

    private BlueprintEntry stored(Long id, String key, AttributeType type, List<String> values) {
        BlueprintEntry e = new BlueprintEntry(id, CAT_ID, key, type, null, values, false, 0);
        fakeLoad.store(CAT_ID, e);
        return e;
    }

    private Command cmd(Long blueprintId, String key, AttributeType ignoredType, List<String> values) {
        return new Command(CAT_ID, blueprintId, key, null, values, false, 0);
    }

    // --- Tests ---

    @Test
    @DisplayName("execute() throws ResourceNotFoundException when blueprint does not exist for category")
    void execute_unknownBlueprint_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(new Command(CAT_ID, 99L, "Material", null, List.of(), false, 0)));
    }

    @Test
    @DisplayName("execute() throws when attributeKey conflicts with another entry in the same category")
    void execute_duplicateKey_throws() {
        stored(1L, "Material", AttributeType.TEXT, List.of());
        stored(2L, "Color", AttributeType.TEXT, List.of());

        // Try to rename entry 1 to the key already used by entry 2
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new Command(CAT_ID, 1L, "Color", null, List.of(), false, 0)));
    }

    @Test
    @DisplayName("execute() allows keeping the same attributeKey on the same entry")
    void execute_sameKey_succeeds() {
        stored(1L, "Material", AttributeType.TEXT, List.of());

        assertDoesNotThrow(() ->
                service.execute(new Command(CAT_ID, 1L, "Material", null, List.of(), false, 0)));
    }

    @Test
    @DisplayName("execute() throws when allowedValues provided for non-ENUM/MULTI type")
    void execute_allowedValuesForText_throws() {
        stored(1L, "Weight", AttributeType.TEXT, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new Command(CAT_ID, 1L, "Weight", null, List.of("Heavy", "Light"), false, 0)));
    }

    @Test
    @DisplayName("execute() throws when unitName provided for non-NUMBER type")
    void execute_unitNameForEnum_throws() {
        stored(1L, "Fit", AttributeType.ENUM, List.of("Slim", "Regular"));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new Command(CAT_ID, 1L, "Fit", "kg", List.of("Slim", "Regular"), false, 0)));
    }

    @Test
    @DisplayName("execute() persists update correctly for TEXT type")
    void execute_happyPath_text() {
        stored(1L, "Material", AttributeType.TEXT, List.of());

        service.execute(new Command(CAT_ID, 1L, "Fabric", null, List.of(), true, 2));

        assertNotNull(fakeSave.lastCommand);
        assertEquals("Fabric", fakeSave.lastCommand.attributeKey());
        assertTrue(fakeSave.lastCommand.required());
        assertEquals(2, fakeSave.lastCommand.sortOrder());
    }

    @Test
    @DisplayName("execute() reconciles allowedValues correctly for ENUM type")
    void execute_happyPath_enum() {
        stored(1L, "Fit", AttributeType.ENUM, List.of("Slim", "Regular"));

        service.execute(new Command(CAT_ID, 1L, "Fit", null, List.of("Slim", "Oversized"), false, 0));

        assertNotNull(fakeSave.lastCommand);
        assertEquals(List.of("Slim", "Oversized"), fakeSave.lastCommand.allowedValues());
    }

    @Test
    @DisplayName("execute() accepts unitName for NUMBER type")
    void execute_happyPath_number() {
        stored(1L, "Weight", AttributeType.NUMBER, List.of());

        assertDoesNotThrow(() ->
                service.execute(new Command(CAT_ID, 1L, "Weight", "kg", List.of(), false, 1)));

        assertEquals("kg", fakeSave.lastCommand.unitName());
    }

    @Test
    @DisplayName("execute() accepts allowedValues for MULTI type")
    void execute_happyPath_multi() {
        stored(1L, "Colors", AttributeType.MULTI, List.of("Red", "Blue"));

        assertDoesNotThrow(() ->
                service.execute(new Command(CAT_ID, 1L, "Colors", null, List.of("Red", "Green"), false, 0)));

        assertEquals(List.of("Red", "Green"), fakeSave.lastCommand.allowedValues());
    }

    // =========================================================
    //  Fakes
    // =========================================================

    static class FakeLoadBlueprintPort implements LoadCategoryBlueprintPort {
        private final List<BlueprintEntry> entries = new ArrayList<>();

        void store(Long categoryId, BlueprintEntry entry) {
            entries.add(entry);
        }

        @Override
        public List<BlueprintEntry> findByCategoryId(Long categoryId) {
            return entries.stream().filter(e -> e.categoryId().equals(categoryId)).toList();
        }
    }

    static class FakeSaveBlueprintPort implements SaveCategoryBlueprintPort {
        UpdateCategoryBlueprintUseCase.Command lastCommand;

        @Override
        public Long save(Long categoryId, String attributeKey, AttributeType type, String unitName,
                         List<String> allowedValues, boolean required, int sortOrder) {
            return 100L;
        }

        @Override
        public void update(UpdateCategoryBlueprintUseCase.Command command) {
            lastCommand = command;
        }

        @Override
        public void deleteById(Long blueprintId) {}
    }
}
