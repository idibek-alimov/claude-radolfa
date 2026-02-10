package tj.radolfa.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.domain.model.ProductBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc test for {@link ErpSyncController}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Valid hierarchy payload → 200 with {synced: 1, errors: 0}</li>
 *   <li>Use case exception → 200 with {synced: 0, errors: 1}</li>
 *   <li>JSON serialization of the response DTO</li>
 * </ul>
 *
 * <p>Note: Security (RBAC) is tested via the Spring Security filter chain
 * in {@code SecurityConfig}, which is already integration-tested through
 * the existing auth tests. This test focuses on the controller logic itself.
 */
class ErpSyncControllerTest {

    private MockMvc mockMvc;
    private FakeSyncUseCase fakeSyncUseCase;
    private FakeLogPort fakeLogPort;

    @BeforeEach
    void setUp() {
        fakeSyncUseCase = new FakeSyncUseCase();
        fakeLogPort = new FakeLogPort();

        ErpSyncController controller = new ErpSyncController(fakeSyncUseCase, fakeLogPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static final String VALID_PAYLOAD = """
            {
              "templateCode": "TPL-001",
              "templateName": "Classic T-Shirt",
              "variants": [
                {
                  "colorKey": "red",
                  "items": [
                    {
                      "erpItemCode": "TPL-001-RED-S",
                      "sizeLabel": "S",
                      "stockQuantity": 50,
                      "price": {
                        "list": 29.99,
                        "effective": 24.99
                      }
                    }
                  ]
                }
              ]
            }
            """;

    @Test
    @DisplayName("Valid payload returns 200 with synced=1, errors=0")
    void validPayload_returns200WithSyncResult() throws Exception {
        mockMvc.perform(post("/api/v1/sync/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(1))
                .andExpect(jsonPath("$.errors").value(0));

        // Verify the use case was invoked
        assert fakeSyncUseCase.lastCommand != null;
        assert "TPL-001".equals(fakeSyncUseCase.lastCommand.templateCode());
        assert "Classic T-Shirt".equals(fakeSyncUseCase.lastCommand.templateName());
        assert fakeSyncUseCase.lastCommand.variants().size() == 1;
        assert fakeSyncUseCase.lastCommand.variants().get(0).items().size() == 1;

        // Verify sync event was logged
        assert fakeLogPort.lastErpId.equals("TPL-001");
        assert fakeLogPort.lastSuccess;
    }

    @Test
    @DisplayName("Use case exception returns 200 with synced=0, errors=1")
    void useCaseFailure_returns200WithError() throws Exception {
        fakeSyncUseCase.shouldThrow = true;

        mockMvc.perform(post("/api/v1/sync/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(0))
                .andExpect(jsonPath("$.errors").value(1));

        // Verify failure was logged
        assert !fakeLogPort.lastSuccess;
        assert fakeLogPort.lastError != null;
    }

    @Test
    @DisplayName("Multi-variant payload is deserialized correctly")
    void multiVariantPayload_deserializesCorrectly() throws Exception {
        String multiPayload = """
                {
                  "templateCode": "TPL-002",
                  "templateName": "Denim Jacket",
                  "variants": [
                    {
                      "colorKey": "blue",
                      "items": [
                        {"erpItemCode": "J-BLUE-S", "sizeLabel": "S", "stockQuantity": 10, "price": {"list": 89.99, "effective": 79.99}},
                        {"erpItemCode": "J-BLUE-M", "sizeLabel": "M", "stockQuantity": 15, "price": {"list": 89.99, "effective": 79.99}}
                      ]
                    },
                    {
                      "colorKey": "black",
                      "items": [
                        {"erpItemCode": "J-BLACK-L", "sizeLabel": "L", "stockQuantity": 8, "price": {"list": 89.99, "effective": 89.99}}
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/sync/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(multiPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(1));

        assert fakeSyncUseCase.lastCommand.variants().size() == 2;
        assert fakeSyncUseCase.lastCommand.variants().get(0).items().size() == 2;
        assert fakeSyncUseCase.lastCommand.variants().get(1).items().size() == 1;
    }

    // ==== Fakes ====

    static class FakeSyncUseCase implements SyncProductHierarchyUseCase {
        HierarchySyncCommand lastCommand;
        boolean shouldThrow;

        @Override
        public ProductBase execute(HierarchySyncCommand command) {
            lastCommand = command;
            if (shouldThrow) throw new RuntimeException("Simulated failure");
            return new ProductBase(1L, command.templateCode(), command.templateName());
        }
    }

    static class FakeLogPort implements LogSyncEventPort {
        String lastErpId;
        boolean lastSuccess;
        String lastError;

        @Override
        public void log(String erpId, boolean success, String error) {
            this.lastErpId = erpId;
            this.lastSuccess = success;
            this.lastError = error;
        }
    }
}
