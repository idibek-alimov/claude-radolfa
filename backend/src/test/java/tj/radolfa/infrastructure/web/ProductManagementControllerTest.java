package tj.radolfa.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductCategoryUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductNameUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.in.product.UpdateSkuSizeLabelUseCase;
import tj.radolfa.domain.model.Money;

/**
 * Standalone MockMvc test for {@link ProductManagementController}.
 *
 * <p>Uses in-memory fakes (no Spring context, no security filter chain).
 * Tests are @Disabled pending Plan 01-01 production code changes
 * (CreateProductResponseDto with slug + variantId).
 */
class ProductManagementControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FakeCreateProductUseCase fakeCreate = new FakeCreateProductUseCase();
        FakeUpdateProductPriceUseCase fakePrice = new FakeUpdateProductPriceUseCase();
        FakeUpdateProductStockUseCase fakeStock = new FakeUpdateProductStockUseCase();
        FakeUpdateProductNameUseCase fakeName = new FakeUpdateProductNameUseCase();
        FakeUpdateSkuSizeLabelUseCase fakeSizeLabel = new FakeUpdateSkuSizeLabelUseCase();
        FakeUpdateProductCategoryUseCase fakeCategory = new FakeUpdateProductCategoryUseCase();

        ProductManagementController controller = new ProductManagementController(
                fakeCreate, fakePrice, fakeStock, fakeName, fakeSizeLabel, fakeCategory
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @Disabled("Awaiting Plan 01-01 production code changes")
    @DisplayName("POST /api/v1/admin/products returns 201 with productBaseId, variantId, slug")
    void createProduct_returns201WithFullResponse() throws Exception {
        // TODO: After Plan 01-01 changes response to CreateProductResponseDto:
        // mockMvc.perform(post("/api/v1/admin/products")
        //     .contentType(MediaType.APPLICATION_JSON)
        //     .content("""
        //         {"name":"Test","categoryId":1,"colorId":1,"skus":[{"sizeLabel":"M","price":10.00,"stockQuantity":5}]}
        //     """))
        //     .andExpect(status().isCreated())
        //     .andExpect(jsonPath("$.productBaseId").isNumber())
        //     .andExpect(jsonPath("$.variantId").isNumber())
        //     .andExpect(jsonPath("$.slug").isString());
    }

    @Test
    @Disabled("Awaiting Plan 01-01 production code changes")
    @DisplayName("POST /api/v1/admin/products accepts optional webDescription")
    void createProduct_acceptsWebDescription() throws Exception {
        // TODO: After Plan 01-01 adds webDescription to request DTO:
        // mockMvc.perform(post("/api/v1/admin/products")
        //     .contentType(MediaType.APPLICATION_JSON)
        //     .content("""
        //         {"name":"Test","categoryId":1,"colorId":1,"webDescription":"<p>Hello</p>","skus":[{"sizeLabel":"M","price":10.00,"stockQuantity":5}]}
        //     """))
        //     .andExpect(status().isCreated());
    }

    // ==== In-Memory Fakes ====

    static class FakeCreateProductUseCase implements CreateProductUseCase {
        @Override
        public Result execute(Command command) {
            return new Result(42L, 10L, "test-product-red");
        }
    }

    static class FakeUpdateProductPriceUseCase implements UpdateProductPriceUseCase {
        @Override
        public void execute(Long skuId, Money newPrice) {
            // no-op
        }
    }

    static class FakeUpdateProductStockUseCase implements UpdateProductStockUseCase {
        @Override
        public void setAbsolute(Long skuId, int quantity) {
            // no-op
        }

        @Override
        public void adjust(Long skuId, int delta) {
            // no-op
        }
    }

    static class FakeUpdateProductNameUseCase implements UpdateProductNameUseCase {
        @Override
        public void execute(Long productBaseId, String newName) {
            // no-op
        }
    }

    static class FakeUpdateSkuSizeLabelUseCase implements UpdateSkuSizeLabelUseCase {
        @Override
        public void execute(Long skuId, String newSizeLabel) {
            // no-op
        }
    }

    static class FakeUpdateProductCategoryUseCase implements UpdateProductCategoryUseCase {
        @Override
        public void execute(Long productBaseId, Long categoryId) {
            // no-op
        }
    }
}
