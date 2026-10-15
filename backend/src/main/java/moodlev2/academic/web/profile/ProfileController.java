package moodlev2.academic.web.profile;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.application.profile.GetMeService;
import moodlev2.academic.web.profile.dto.UserProfileDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's profile with their courses. The rest of {@code /api/users/**} (password,
 * sessions, picture) belongs to identity-service; the gateway routes only this path here.
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final GetMeService getMeService;

    /** The token's subject is the user id. */
    @GetMapping("/api/users/me")
    public UserProfileDto getMyProfile(@AuthenticationPrincipal Jwt token) {
        return getMeService.getProfile(Long.parseLong(token.getSubject()));
    }
}
