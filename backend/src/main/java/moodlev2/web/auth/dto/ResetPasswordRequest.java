package moodlev2.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 128)
    private String token;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 128)
    private String newPassword;
}
