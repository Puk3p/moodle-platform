package moodlev2.identity.api;

/**
 * An account as its owner sees it: the directory entry plus the security settings only the owner's
 * own profile shows.
 */
public record AccountSummary(UserSummary user, boolean twoFaEnabled) {}
