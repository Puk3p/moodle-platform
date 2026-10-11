package moodlev2.identity.api;

import java.util.List;

/**
 * Sessions that no longer exist; listeners close anything still using them (open sockets).
 *
 * @param userId whose sessions they were
 * @param tokenHashes the SHA-256 of each revoked session token
 */
public record SessionsRevokedEvent(long userId, List<String> tokenHashes) {}
