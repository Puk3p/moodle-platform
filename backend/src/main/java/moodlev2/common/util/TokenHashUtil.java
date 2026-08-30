package moodlev2.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing JWT tokens into fixed-length, collision-resistant identifiers suitable for
 * database storage and lookup. Uses SHA-256 to produce a 64-character hex string.
 */
public final class TokenHashUtil {

    private TokenHashUtil() {}

    /**
     * Returns the SHA-256 hex digest of the given token string.
     *
     * @param token the full JWT token
     * @return 64-character lowercase hex string
     */
    public static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this should never happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
