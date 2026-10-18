package moodlev2.identity.api;

/**
 * An account's email changed. Published inside the transaction that changes it; modules that stored
 * the old address (chat refers to people by email) update theirs.
 */
public record UserEmailChangedEvent(long userId, String oldEmail, String newEmail) {}
