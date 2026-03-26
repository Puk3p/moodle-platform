package moodlev2.web.auth.dto;

import java.util.Set;
import moodlev2.domain.user.Role;

public record AuthResponse(
        String accessToken,
        String userId,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles,
        boolean requiresTwoFa) {}
