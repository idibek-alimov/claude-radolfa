package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.domain.model.ListingVariant;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ReorderVariantImagesService}.
 *
 * <p>No Spring context, no Mockito — hand-written in-memory fake adapter.
 */
class ReorderVariantImagesServiceTest {

    private FakeSaveListingVariantPort fakePort;
    private ReorderVariantImagesService service;

    @BeforeEach
    void setUp() {
        fakePort = new FakeSaveListingVariantPort();
        service  = new ReorderVariantImagesService(fakePort);
    }

    // =========================================================
    //  Happy-path
    // =========================================================

    @Test
    @DisplayName("Delegates to the port with correct arguments")
    void execute_delegatesToPort() {
        List<Long> orderedIds = List.of(3L, 1L, 2L);

        service.execute(10L, orderedIds);

        assertEquals(10L, fakePort.capturedVariantId);
        assertEquals(orderedIds, fakePort.capturedImageIds);
    }

    @Test
    @DisplayName("Completes without exception for a single-image list")
    void execute_singleImage_succeeds() {
        assertDoesNotThrow(() -> service.execute(5L, List.of(99L)));
    }

    // =========================================================
    //  Error paths — propagated from the port
    // =========================================================

    @Test
    @DisplayName("Propagates IllegalArgumentException when imageIds mismatch")
    void execute_mismatchedIds_propagatesException() {
        fakePort.throwOnReorder = true;

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(10L, List.of(1L, 2L, 99L)));
    }

    // =========================================================
    //  Fake
    // =========================================================

    static class FakeSaveListingVariantPort implements SaveListingVariantPort {
        Long capturedVariantId;
        List<Long> capturedImageIds;
        boolean throwOnReorder = false;

        @Override
        public void save(ListingVariant variant) {}

        @Override
        public void saveTags(Long variantId, List<Long> tagIds) {}

        @Override
        public void reorderImages(Long variantId, List<Long> orderedImageIds) {
            if (throwOnReorder) {
                throw new IllegalArgumentException(
                        "imageIds do not match existing images for variantId=" + variantId);
            }
            capturedVariantId = variantId;
            capturedImageIds  = new ArrayList<>(orderedImageIds);
        }
    }
}
