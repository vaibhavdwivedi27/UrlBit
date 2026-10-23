package in.urlbit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Email is required")
                @Email(message = "Invalid email format")
                @Size(max = 320, message = "Email exceeds maximum length")
                String email,
        @NotBlank(message = "Password is required")
                @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
                String password) {}
