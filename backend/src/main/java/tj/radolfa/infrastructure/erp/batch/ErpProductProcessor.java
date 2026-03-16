package tj.radolfa.infrastructure.erp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.SyncProductUseCase;
import tj.radolfa.application.ports.in.SyncProductUseCase.SyncProductCommand;
import tj.radolfa.application.ports.in.SyncProductUseCase.SyncProductCommand.VariantCommand;
import tj.radolfa.domain.model.ProductTemplate;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Batch processor that groups flat ERP snapshots into
 * template→variants commands and delegates to {@link SyncProductUseCase}.
 *
 * <p>The ERP API returns a flat list of items. This processor handles three cases:
 * <ol>
 *   <li><b>Template item</b> (has_variants=true): Buffered until its variants arrive.
 *       Since batch reads items in order, templates appear before their variants.</li>
 *   <li><b>Variant item</b> (variant_of != null): Grouped under its template.</li>
 *   <li><b>Standalone item</b> (no variants, not a variant): Synced immediately
 *       as a template with zero variants (service creates a default variant).</li>
 * </ol>
 *
 * <p>Note: This processor calls the use case directly. The writer is a no-op logger.
 */
@Component
public class ErpProductProcessor implements ItemProcessor<ErpProductSnapshot, ProductTemplate> {

    private static final Logger LOG = LoggerFactory.getLogger(ErpProductProcessor.class);

    private final SyncProductUseCase syncUseCase;

    // Buffer: templateCode -> (template snapshot, list of variant snapshots)
    private final Map<String, BufferedTemplate> templateBuffer = new LinkedHashMap<>();

    public ErpProductProcessor(SyncProductUseCase syncUseCase) {
        this.syncUseCase = syncUseCase;
    }

    @Override
    public ProductTemplate process(ErpProductSnapshot snapshot) {

        if (snapshot.hasVariants()) {
            // Buffer template — wait for its variants
            templateBuffer.put(snapshot.erpItemCode(), new BufferedTemplate(snapshot, new ArrayList<>()));
            LOG.debug("[BATCH] Buffered template={}", snapshot.erpItemCode());
            return null; // Spring Batch skips null
        }

        if (snapshot.variantOf() != null && !snapshot.variantOf().isBlank()) {
            // This is a variant — attach to its buffered template
            BufferedTemplate parent = templateBuffer.get(snapshot.variantOf());
            if (parent != null) {
                parent.variants.add(snapshot);
                LOG.debug("[BATCH] Attached variant={} to template={}", snapshot.erpItemCode(), snapshot.variantOf());

                // Flush immediately: we can't know if more variants will come,
                // but the use case is idempotent — re-syncing is safe
                return flushTemplate(parent);
            } else {
                // Orphan variant — template wasn't in this page. Sync as standalone.
                LOG.warn("[BATCH] Orphan variant={} (template={} not buffered), syncing standalone",
                        snapshot.erpItemCode(), snapshot.variantOf());
                return syncStandalone(snapshot);
            }
        }

        // Standalone item — no variants, not a variant
        return syncStandalone(snapshot);
    }

    private ProductTemplate flushTemplate(BufferedTemplate buffered) {
        ErpProductSnapshot tmpl = buffered.template;

        List<VariantCommand> variantCommands = buffered.variants.stream()
                .map(v -> new VariantCommand(
                        v.erpItemCode(),
                        v.attributes(),
                        v.standardRate(),
                        v.stock(),
                        v.disabled()
                ))
                .toList();

        var command = new SyncProductCommand(
                tmpl.erpItemCode(),
                tmpl.name(),
                tmpl.category(),
                tmpl.disabled(),
                variantCommands
        );

        return syncUseCase.execute(command);
    }

    private ProductTemplate syncStandalone(ErpProductSnapshot snapshot) {
        // Standalone: one variant whose code equals the template code
        var variantCommand = new VariantCommand(
                snapshot.erpItemCode(),
                snapshot.attributes(),
                snapshot.standardRate(),
                snapshot.stock(),
                snapshot.disabled()
        );

        var command = new SyncProductCommand(
                snapshot.erpItemCode(),
                snapshot.name(),
                snapshot.category(),
                snapshot.disabled(),
                List.of(variantCommand)
        );

        return syncUseCase.execute(command);
    }

    private record BufferedTemplate(ErpProductSnapshot template, List<ErpProductSnapshot> variants) {}
}
