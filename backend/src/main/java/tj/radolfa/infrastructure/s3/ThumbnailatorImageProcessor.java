package tj.radolfa.infrastructure.s3;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ImageProcessingPort;
import tj.radolfa.application.ports.out.ProcessedImage;
import tj.radolfa.domain.exception.ImageProcessingException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Infrastructure adapter: resize and compress images using Thumbnailator.
 *
 * Rules enforced here:
 *   - Maximum width is 1920 px.  Images narrower than that are NOT upscaled.
 *   - Target format is WebP at quality 0.8.
 *   - WebP availability is probed once at construction time via
 *     {@link ImageIO#getImageWritersByFormatName}.  If the JVM does not ship
 *     a WebP codec (standard OpenJDK 17 does not), the processor falls back
 *     to JPEG at the same quality and logs a single WARN.
 *
 * This is the ONLY class in the project that imports {@code net.coobird}.
 */
@Component
public class ThumbnailatorImageProcessor implements ImageProcessingPort {

    private static final Logger LOG       = LoggerFactory.getLogger(ThumbnailatorImageProcessor.class);
    private static final int    MAX_WIDTH = 1920;
    private static final float  QUALITY   = 0.8f;

    /** Cached on first construction -- format availability does not change at runtime. */
    private final boolean webpSupported;

    public ThumbnailatorImageProcessor() {
        this.webpSupported = ImageIO.getImageWritersByFormatName("webp").hasNext();
        if (!webpSupported) {
            LOG.warn("[IMAGE] WebP writer not available on this JVM. Falling back to JPEG.");
        }
    }

    @Override
    public ProcessedImage process(InputStream source, String originalFilename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            String format = webpSupported ? "webp" : "jpg";
            String mime   = webpSupported ? "image/webp" : "image/jpeg";

            Thumbnails.of(source)
                    .size(MAX_WIDTH, MAX_WIDTH)
                    .keepAspectRatio(true)
                    .outputFormat(format)
                    .outputQuality(QUALITY)
                    .toOutputStream(out);

            return new ProcessedImage(
                    new ByteArrayInputStream(out.toByteArray()),
                    mime,
                    format.equals("webp") ? "webp" : "jpg"
            );
        } catch (IOException ex) {
            throw new ImageProcessingException("Failed to process image: " + originalFilename, ex);
        }
    }
}
