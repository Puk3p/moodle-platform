package moodlev2.web.user;

import lombok.RequiredArgsConstructor;
import moodlev2.application.user.GetMeService;
import moodlev2.web.user.dto.UserProfileDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetMeService getMeUseCase;

    @GetMapping("/me")
    public UserProfileDto getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return getMeUseCase.getCurrentUserProfile(email);
    }
}