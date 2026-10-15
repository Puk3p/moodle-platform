package moodlev2.academic.directory;

/**
 * An account as its owner sees it: the directory entry plus the security settings only the owner's
 * own profile shows.
 */
public record AccountSummary(UserSummary user, boolean twoFaEnabled) {}
