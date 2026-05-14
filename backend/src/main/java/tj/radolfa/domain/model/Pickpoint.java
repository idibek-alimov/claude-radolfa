package tj.radolfa.domain.model;

public record Pickpoint(
        Long id,
        String name,
        String address,
        boolean active,
        Double latitude,
        Double longitude,
        boolean hasParking,
        boolean hasFittingRoom,
        boolean hasCardPayment,
        boolean wheelchairAccessible,
        String timezone,
        boolean temporarilyClosed
) {}
