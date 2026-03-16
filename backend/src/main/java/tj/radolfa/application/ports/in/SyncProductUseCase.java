package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.ProductTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * In-Port: synchronise a product template and its variants from ERPNext.
 *
 * <p>Accepts a template with its flat list of variants (each variant is a
 * purchasable item with concrete attribute values like Color + Size).
 *
 * <p>Called by the SYSTEM role (ERP sync endpoint) and by the batch job.
 */
public interface SyncProductUseCase {

    ProductTemplate execute(SyncProductCommand command);

    record SyncProductCommand(
            String templateCode,
            String templateName,
            String category,
            boolean disabled,
            List<VariantCommand> variants
    ) {
        public record VariantCommand(
                String erpVariantCode,
                Map<String, String> attributes,
                BigDecimal price,
                Integer stockQty,
                boolean disabled
        ) {}
    }
}
