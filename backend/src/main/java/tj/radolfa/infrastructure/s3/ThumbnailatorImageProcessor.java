package tj.radolfa.infrastructure.s3;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ImageProcessingPort;
import tj.radolfa.application.ports.out.ProcessedImage;
import tj.radolfa.domain.exception.ImageProcessingException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
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
 *   - Output is encoded progressively when the writer supports it (JPEG does,
 *     WebP does not). Browsers then paint a low-resolution preview that
 *     sharpens as bytes arrive, instead of rendering the image top-to-bottom.
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
            String format = webpSupported ? "webp" : "jpg";
            String mime   = webpSupported ? "image/webp" : "image/jpeg";

            // JPEG cannot encode an alpha channel ("Bogus input colorspace"); WebP can.
            // Without this, a resized PNG/GIF with transparency comes back as TYPE_INT_ARGB
            // and fails at write time, so pin the pixel type up front to match the format.
            int imageType = format.equals("webp") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

            BufferedImage resized = Thumbnails.of(source)
                    .size(MAX_WIDTH, MAX_WIDTH)
                    .keepAspectRatio(true)
                    .imageType(imageType)
                    .asBufferedImage();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            encode(resized, format, out);

            return new ProcessedImage(
                    new ByteArrayInputStream(out.toByteArray()),
                    mime,
                    format.equals("webp") ? "webp" : "jpg"
            );
        } catch (IOException ex) {
            throw new ImageProcessingException("Failed to process image: " + originalFilename, ex);
        }
    }

    /**
     * Encodes via the format's {@link ImageWriter} directly -- rather than Thumbnailator's
     * {@code toOutputStream} -- so that progressive mode can be enabled when the writer
     * supports it (it cannot be configured through Thumbnailator's fluent API).
     */
    private void encode(BufferedImage image, String format, ByteArrayOutputStream out) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            if (param.canWriteProgressive()) {
                param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            }

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }
}
