package moodlev2.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The one address browsers talk to. It routes each path to the service that owns it, and for the
 * services split out of core it swaps the browser's session cookie for a short-lived signed token
 * (core keeps authenticating its own routes as before). See {@code application.yml} for the routes.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
