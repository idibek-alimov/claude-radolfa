package tj.radolfa.domain.model;

public record FeaturedCategory(
        Long id,
        Long categoryId,
        String imageUrl,
        String title,
        String subtitle,
        int displayOrder,
        boolean active
) {
    public FeaturedCategory {
        if (categoryId == null) throw new IllegalArgumentException("categoryId must not be null");
        if (displayOrder < 0) throw new IllegalArgumentException("displayOrder must be >= 0");
    }
}
