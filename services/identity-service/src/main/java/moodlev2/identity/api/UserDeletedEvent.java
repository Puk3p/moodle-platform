package moodlev2.identity.api;

/**
 * An account is being deleted. Published inside the deleting transaction, before the row goes, so
 * modules can remove what they keep about the person.
 */
public record UserDeletedEvent(long userId, String email) {}
