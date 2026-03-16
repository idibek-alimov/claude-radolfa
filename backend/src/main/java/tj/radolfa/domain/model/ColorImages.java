package tj.radolfa.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Image gallery for a specific (template, color) combination.
 *
 * <p>{@code colorKey} is null for standalone / colorless products.
 * Enrichment-owned — never overwritten by ERP sync.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ColorImages {

    private static final int MAX_IMAGES = 20;

    private final Long   id;
    private final Long   templateId;
    private final String colorKey;

    private final List<String> images;

    public ColorImages(Long id, Long templateId, String colorKey, List<String> images) {
        if (templateId == null) {
            throw new IllegalArgumentException("templateId must not be null");
        }
        this.id         = id;
        this.templateId = templateId;
        this.colorKey   = colorKey;
        this.images     = new ArrayList<>(images != null ? images : List.of());
    }

    public void addImage(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Image URL must not be blank");
        }
        if (this.images.size() >= MAX_IMAGES) {
            throw new IllegalStateException("Maximum of " + MAX_IMAGES + " images reached");
        }
        this.images.add(url);
    }

    public void removeImage(String url) {
        this.images.remove(url);
    }

    public Long          getId()         { return id; }
    public Long          getTemplateId() { return templateId; }
    public String        getColorKey()   { return colorKey; }
    public List<String>  getImages()     { return Collections.unmodifiableList(images); }
}
