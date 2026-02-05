package tj.radolfa.infrastructure.web.dto;

/**
 * Generic message response DTO for simple status messages.
 */
public record MessageResponseDto(
        String message,
        boolean success
) {
    public static MessageResponseDto success(String message) {
        return new MessageResponseDto(message, true);
    }

    public static MessageResponseDto error(String message) {
        return new MessageResponseDto(message, false);
    }
}
