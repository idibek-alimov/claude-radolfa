package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneChangeVerifyDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "OTP is required")
        String otp) {
}
