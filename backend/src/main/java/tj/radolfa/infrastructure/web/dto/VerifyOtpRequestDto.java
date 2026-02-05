package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for OTP verification request (step 2: verify OTP, get token).
 */
public record VerifyOtpRequestDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{4,6}$", message = "Invalid OTP format")
        String otp
) {}
