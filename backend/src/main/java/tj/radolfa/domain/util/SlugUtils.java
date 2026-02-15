package tj.radolfa.domain.util;

/**
 * Stateless utility for generating URL-safe slugs from arbitrary strings.
 * Pure Java — no external dependencies.
 */
public final class SlugUtils {

    private SlugUtils() {}

    /**
     * Converts a name into a URL-safe slug.
     * Example: "Men's T-Shirts" → "men-s-t-shirts"
     */
    public static String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
