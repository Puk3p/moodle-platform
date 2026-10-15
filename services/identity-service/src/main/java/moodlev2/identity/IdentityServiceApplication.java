package moodlev2.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Accounts, classes, sign-in (password, 2FA, Google/Facebook) and browser sessions. The only
 * service that reads the session cookie: it also issues the internal tokens every other service
 * trusts, and answers the gateway's session-to-token exchange.
 */
@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
