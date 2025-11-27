package moodlev2.web.auth.dto;

import moodlev2.domain.user.Role;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String userId,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles
) {}