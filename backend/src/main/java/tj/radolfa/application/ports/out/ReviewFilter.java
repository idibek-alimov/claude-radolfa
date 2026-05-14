package tj.radolfa.application.ports.out;

public record ReviewFilter(boolean hasPhotos, Integer rating, String search) {

    public static ReviewFilter empty() {
        return new ReviewFilter(false, null, null);
    }
}
