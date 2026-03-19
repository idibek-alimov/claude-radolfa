package tj.radolfa.infrastructure.importer.batch;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

import tj.radolfa.infrastructure.importer.ProductImportClient;
import tj.radolfa.infrastructure.importer.ImportedProductSnapshot;

import java.util.Iterator;
import java.util.List;

/**
 * Spring Batch {@code ItemReader} that pages through {@link ProductImportClient}.
 *
 * One page is buffered in memory at a time.  When the buffer is drained the
 * reader fetches the next page.  A {@code null} return from {@link #read()}
 * signals end-of-input to the Spring Batch framework.
 */
@Component
public class ImportedProductReader extends AbstractItemStreamItemReader<ImportedProductSnapshot> {

    private static final int PAGE_SIZE = 100;
    private static final String KEY_CURRENT_PAGE = "import.reader.currentPage";

    private final ProductImportClient client;

    private int              currentPage = 1;
    private Iterator<ImportedProductSnapshot> buffer = List.<ImportedProductSnapshot>of().iterator();
    private boolean          exhausted    = false;

    public ImportedProductReader(ProductImportClient client) {
        this.client = client;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        super.open(executionContext);
        // Always reset state so the reader works across repeated job launches
        currentPage = 1;
        buffer      = List.<ImportedProductSnapshot>of().iterator();
        exhausted   = false;

        if (executionContext.containsKey(KEY_CURRENT_PAGE)) {
            currentPage = executionContext.getInt(KEY_CURRENT_PAGE);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        super.update(executionContext);
        executionContext.putInt(KEY_CURRENT_PAGE, currentPage);
    }

    @Override
    public ImportedProductSnapshot read() {
        while (!buffer.hasNext()) {
            if (exhausted) {
                return null;   // end-of-input
            }
            List<ImportedProductSnapshot> page = client.fetchPage(currentPage, PAGE_SIZE);
            if (page.isEmpty()) {
                exhausted = true;
                return null;   // end-of-input
            }
            buffer = page.iterator();
            currentPage++;
        }
        return buffer.next();
    }
}
