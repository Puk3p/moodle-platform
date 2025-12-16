package moodlev2.web.user;

import lombok.RequiredArgsConstructor;
import moodlev2.application.user.ChangePasswordService;
import moodlev2.application.user.GetMeService;
import moodlev2.web.user.dto.ChangePasswordRequest;
import moodlev2.web.user.dto.UserProfileDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetMeService getMeUseCase;
    private final ChangePasswordService changePasswordService;

    @GetMapping("/me")
    public UserProfileDto getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return getMeUseCase.getCurrentUserProfile(email);
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        changePasswordService.changePassword(authentication.getName(), request);
    }
}