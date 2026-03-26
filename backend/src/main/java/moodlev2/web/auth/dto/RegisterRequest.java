package moodlev2.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {
    @Email @NotBlank public String email;
    @NotBlank public String password;
    @NotBlank public String firstName;
    @NotBlank public String lastName;
}
