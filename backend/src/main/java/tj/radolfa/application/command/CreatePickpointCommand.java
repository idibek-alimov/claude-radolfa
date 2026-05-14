package tj.radolfa.application.command;

public record CreatePickpointCommand(
        String name,
        String address,
        Double latitude,
        Double longitude,
        boolean hasParking,
        boolean hasFittingRoom,
        boolean hasCardPayment,
        boolean wheelchairAccessible
) {}
