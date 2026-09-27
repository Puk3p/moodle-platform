package moodlev2.web.chat.dto;

import java.time.Instant;

/**
 * Whether messaging is currently available to the caller. The client hides the chat entirely when
 * it is not; the server refuses every chat call regardless of what the client shows.
 *
 * @param reason {@code null} when available; otherwise {@link #NOT_PERMITTED} or {@link
 *     #QUIZ_IN_PROGRESS}
 * @param lockedUntil when a quiz lock lapses on its own, so the client knows when to re-check
 */
public record ChatStatusDto(boolean available, String reason, Instant lockedUntil) {

    public static final String NOT_PERMITTED = "NOT_PERMITTED";
    public static final String QUIZ_IN_PROGRESS = "QUIZ_IN_PROGRESS";

    public static ChatStatusDto open() {
        return new ChatStatusDto(true, null, null);
    }

    public static ChatStatusDto notPermitted() {
        return new ChatStatusDto(false, NOT_PERMITTED, null);
    }

    public static ChatStatusDto lockedUntil(Instant until) {
        return new ChatStatusDto(false, QUIZ_IN_PROGRESS, until);
    }
}
