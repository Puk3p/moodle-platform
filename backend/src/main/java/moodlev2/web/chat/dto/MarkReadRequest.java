package moodlev2.web.chat.dto;

/**
 * Marks the caller's conversation with {@code partner} read up to and including message {@code
 * upToId}. The reader is taken from the authenticated caller.
 */
public record MarkReadRequest(String partner, Long upToId) {}
