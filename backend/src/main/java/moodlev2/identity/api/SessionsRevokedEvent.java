package moodlev2.identity.api;

import java.util.List;

/** Hashes of sessions that no longer exist; listeners close anything still using them. */
public record SessionsRevokedEvent(List<String> tokenHashes) {}
