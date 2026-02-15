package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequestDto(
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email) {
}
