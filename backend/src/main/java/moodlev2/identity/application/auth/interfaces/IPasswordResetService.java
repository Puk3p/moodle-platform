package moodlev2.identity.application.auth.interfaces;

public interface IPasswordResetService {
    void processForgotPassword(String email);

    void resetPassword(String token, String newPassword);

    void sendEmail(String to, String token);
}
