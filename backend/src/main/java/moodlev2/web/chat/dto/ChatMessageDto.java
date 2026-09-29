package moodlev2.web.chat.dto;

import java.time.Instant;

/**
 * One message, as seen by one participant. {@code sender} is always the authenticated author, never
 * client-supplied. {@code read} is whether the viewer has seen it: always true for their own
 * messages, so it never tells a sender whether the other side has read theirs.
 */
public record ChatMessageDto(
        Long id,
        String sender,
        String recipient,
        String content,
        Instant timestamp,
        boolean read) {}
