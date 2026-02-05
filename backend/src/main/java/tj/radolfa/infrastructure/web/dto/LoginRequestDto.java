package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for login request (step 1: request OTP).
 */
public record LoginRequestDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
        String phone
) {}
