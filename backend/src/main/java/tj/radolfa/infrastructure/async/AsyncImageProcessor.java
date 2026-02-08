package tj.radolfa.infrastructure.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.AddProductImageUseCase;
import tj.radolfa.domain.exception.ImageProcessingException;

import java.io.ByteArrayInputStream;

/**
 * Async wrapper around the synchronous image processing pipeline.
 *
 * <p>The caller buffers raw bytes and fires this method. The request thread
 * is released immediately. Resize, S3 upload, and DB persistence all happen
 * on the {@code imageProcessorExecutor} thread pool.
 *
 * <h3>Retry policy</h3>
 * <ul>
 *   <li>S3/IO failures: up to 3 attempts with exponential backoff (1s, 2s, 4s).</li>
 *   <li>{@link ImageProcessingException}: no retry — corrupt bytes won't fix themselves.</li>
 * </ul>
 */
@Component
public class AsyncImageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncImageProcessor.class);
    private static final int MAX_RETRIES = 3;

    private final AddProductImageUseCase addProductImageUseCase;

    public AsyncImageProcessor(AddProductImageUseCase addProductImageUseCase) {
        this.addProductImageUseCase = addProductImageUseCase;
    }

    @Async("imageProcessorExecutor")
    public void processAsync(String erpId, byte[] imageBytes, String originalName) {
        LOG.info("[IMAGE-ASYNC] Starting background processing for erpId={}, size={}KB",
                erpId, imageBytes.length / 1024);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var stream = new ByteArrayInputStream(imageBytes);
                addProductImageUseCase.execute(erpId, stream, originalName);
                LOG.info("[IMAGE-ASYNC] Completed successfully for erpId={}", erpId);
                return;
            } catch (ImageProcessingException ex) {
                LOG.error("[IMAGE-ASYNC] Image processing failed for erpId={}, not retrying: {}",
                        erpId, ex.getMessage());
                return;
            } catch (Exception ex) {
                if (attempt < MAX_RETRIES) {
                    long backoffMs = (1L << (attempt - 1)) * 1000;
                    LOG.warn("[IMAGE-ASYNC] Attempt {}/{} failed for erpId={}, retrying in {}ms",
                            attempt, MAX_RETRIES, erpId, backoffMs, ex);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOG.warn("[IMAGE-ASYNC] Interrupted during backoff for erpId={}", erpId);
                        return;
                    }
                } else {
                    LOG.error("[IMAGE-ASYNC] All {} attempts exhausted for erpId={}",
                            MAX_RETRIES, erpId, ex);
                }
            }
        }
    }
}
