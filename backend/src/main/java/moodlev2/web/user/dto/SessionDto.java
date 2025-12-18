package moodlev2.web.user.dto;

public record SessionDto(
        Long id,
        String deviceName,
        String ipAddress,
        String lastActive,
        boolean isCurrent
) {}