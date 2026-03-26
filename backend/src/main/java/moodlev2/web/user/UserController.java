package moodlev2.web.user;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.user.ChangePasswordService;
import moodlev2.application.user.GetMeService;
import moodlev2.application.user.GetTeachersService;
import moodlev2.application.user.ManageSessionsService;
import moodlev2.web.course.dto.SimpleDto;
import moodlev2.web.user.dto.ChangePasswordRequest;
import moodlev2.web.user.dto.SessionDto;
import moodlev2.web.user.dto.UserProfileDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetMeService getMeUseCase;
    private final ChangePasswordService changePasswordService;

    private final ManageSessionsService manageSessionsService;
    private final GetTeachersService getTeachersService;

    @GetMapping("/me")
    public UserProfileDto getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return getMeUseCase.getCurrentUserProfile(email);
    }

    @PostMapping("/change-password")
    public void changePassword(
            @RequestBody ChangePasswordRequest request, Authentication authentication) {
        changePasswordService.changePassword(authentication.getName(), request);
    }

    @GetMapping("/sessions")
    public List<SessionDto> getActiveSessions(
            Authentication authentication, HttpServletRequest request) {
        String token = extractToken(request);
        return manageSessionsService.getUserSessions(authentication.getName(), token);
    }

    @DeleteMapping("/sessions/{id}")
    public void revokeSession(@PathVariable Long id, Authentication authentication) {
        manageSessionsService.revokeSession(id, authentication.getName());
    }

    @DeleteMapping("/sessions/others")
    public void revokeAllOthers(Authentication authentication, HttpServletRequest request) {
        String token = extractToken(request);
        manageSessionsService.revokeAllOtherSessions(authentication.getName(), token);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return "";
    }

    @GetMapping("/teachers")
    public List<SimpleDto> getTeachers() {
        return getTeachersService.getTeachersList();
    }
}
