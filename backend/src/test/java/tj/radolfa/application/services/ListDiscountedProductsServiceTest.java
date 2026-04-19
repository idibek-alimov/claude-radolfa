package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.DiscountedProductFilter;
import tj.radolfa.application.ports.out.ListDiscountedProductsPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.DiscountSummary;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.DiscountedProductRow;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListDiscountedProductsServiceTest {

    private FakeListDiscountedProductsPort fakePort;
    private ListDiscountedProductsService service;

    @BeforeEach
    void setUp() {
        fakePort = new FakeListDiscountedProductsPort();
        service  = new ListDiscountedProductsService(fakePort);
    }

    @Test
    @DisplayName("Delegates call with filter and pageable to port; returns page unchanged")
    void execute_delegatesToPort() {
        DiscountType type = new DiscountType(1L, "FLASH_SALE", 1);
        DiscountSummary winner = new DiscountSummary(10L, "Flash", "#FF0000", new BigDecimal("20.00"), AmountType.PERCENT, type);
        DiscountedProductRow row = new DiscountedProductRow(
                1L, "SKU-001", "M", 50,
                new BigDecimal("100.00"), new BigDecimal("80.00"), new BigDecimal("20.00"),
                winner, List.of(), 1L, "Product A", 1L, "RD-001", null
        );
        fakePort.page = new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);

        DiscountedProductFilter filter = new DiscountedProductFilter("SKU", null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<DiscountedProductRow> result = service.execute(filter, pageable);

        assertSame(fakePort.page, result);
        assertSame(filter, fakePort.capturedFilter);
        assertSame(pageable, fakePort.capturedPageable);
    }

    @Test
    @DisplayName("Returns empty page when port returns empty page")
    void execute_emptyResult() {
        fakePort.page = Page.empty(PageRequest.of(0, 20));

        Page<DiscountedProductRow> result = service.execute(
                DiscountedProductFilter.empty(), PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
    }

    // ---- Fake ----

    static class FakeListDiscountedProductsPort implements ListDiscountedProductsPort {
        Page<DiscountedProductRow> page = Page.empty();
        DiscountedProductFilter capturedFilter;
        Pageable capturedPageable;

        @Override
        public Page<DiscountedProductRow> findDiscountedProducts(DiscountedProductFilter filter, Pageable pageable) {
            capturedFilter   = filter;
            capturedPageable = pageable;
            return page;
        }
    }
}
