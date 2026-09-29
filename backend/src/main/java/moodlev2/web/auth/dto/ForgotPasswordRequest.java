package moodlev2.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max = 255)
    private String email;
}
