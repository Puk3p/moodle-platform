package moodlev2.identity.web.user;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.application.auth.SessionService;
import moodlev2.identity.application.user.ChangePasswordService;
import moodlev2.identity.application.user.GetMeService;
import moodlev2.identity.application.user.GetTeachersService;
import moodlev2.identity.application.user.ManageSessionsService;
import moodlev2.identity.infrastructure.security.SessionAuthenticationFilter;
import moodlev2.identity.web.user.dto.ChangePasswordRequest;
import moodlev2.identity.web.user.dto.SessionDto;
import moodlev2.identity.web.user.dto.UserProfileDto;
import moodlev2.shared.web.SimpleDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        SessionService.Resolved current = currentSession(httpRequest);
        changePasswordService.changePassword(
                current.user().getEmail(), request, current.tokenHash());
    }

    @GetMapping("/sessions")
    public List<SessionDto> getActiveSessions(HttpServletRequest httpRequest) {
        SessionService.Resolved current = currentSession(httpRequest);
        return manageSessionsService.getUserSessions(
                current.user().getEmail(), current.tokenHash());
    }

    @DeleteMapping("/sessions/{id}")
    public void revokeSession(@PathVariable Long id, HttpServletRequest httpRequest) {
        manageSessionsService.revokeSession(id, currentSession(httpRequest).user().getId());
    }

    @DeleteMapping("/sessions/others")
    public void revokeAllOthers(HttpServletRequest httpRequest) {
        SessionService.Resolved current = currentSession(httpRequest);
        manageSessionsService.revokeAllOtherSessions(current.user().getId(), current.tokenHash());
    }

    /** Set by SessionAuthenticationFilter for every authenticated request. */
    private static SessionService.Resolved currentSession(HttpServletRequest request) {
        if (request.getAttribute(SessionAuthenticationFilter.CURRENT_SESSION)
                instanceof SessionService.Resolved current) {
            return current;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/teachers")
    public List<SimpleDto> getTeachers() {
        return getTeachersService.getTeachersList();
    }
}
