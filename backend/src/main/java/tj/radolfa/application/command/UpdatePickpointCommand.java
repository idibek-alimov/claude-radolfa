package tj.radolfa.application.command;

public record UpdatePickpointCommand(
        Long id,
        String name,
        String address,
        boolean active,
        Double latitude,
        Double longitude,
        boolean hasParking,
        boolean hasFittingRoom,
        boolean hasCardPayment,
        boolean wheelchairAccessible
) {}
