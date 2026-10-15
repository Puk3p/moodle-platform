package moodlev2.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The one address browsers talk to. It routes each path to the service that owns it and, for every
 * service but identity-service (which issues the session cookie and reads it itself), swaps the
 * browser's session cookie for a short-lived signed token. It also answers CORS for all of them.
 * See {@code application.yml} for the routes.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
