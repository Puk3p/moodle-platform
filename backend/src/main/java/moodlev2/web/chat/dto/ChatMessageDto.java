package moodlev2.web.chat.dto;

import java.time.Instant;

/** One message. {@code sender} is always the authenticated author, never client-supplied. */
public record ChatMessageDto(
        Long id, String sender, String recipient, String content, Instant timestamp) {}
