package moodlev2.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @Email @NotBlank public String email;

    @NotBlank
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters.")
    public String password;

    @NotBlank
    @Size(max = 100)
    public String firstName;

    @NotBlank
    @Size(max = 100)
    public String lastName;
}
