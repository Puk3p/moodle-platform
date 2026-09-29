package moodlev2.web.auth.dto;

import java.util.Set;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;

/**
 * Who is signed in. Deliberately carries no token: the session lives only in an HttpOnly cookie.
 */
public record AuthResponse(
        String userId,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles,
        boolean requiresTwoFa) {

    public static AuthResponse of(User user) {
        return new AuthResponse(
                user.getId() == null ? null : user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles(),
                false);
    }

    public static AuthResponse twoFactorRequired() {
        return new AuthResponse(null, null, null, null, Set.of(), true);
    }
}
