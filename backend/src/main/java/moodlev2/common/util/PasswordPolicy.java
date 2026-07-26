package moodlev2.common.util;

/**
 * Central password-strength policy. Enforced server-side on every path that sets a password
 * (registration, reset and change) so the rule cannot be bypassed by calling the API directly.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {}

    /**
     * Validates the supplied raw password, throwing {@link IllegalArgumentException} with a
     * user-facing message when it does not meet the policy.
     */
    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (rawPassword.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at most " + MAX_LENGTH + " characters long.");
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < rawPassword.length(); i++) {
            char c = rawPassword.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("Password must contain both letters and numbers.");
        }
    }
}
