package moodlev2.gateway.session;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * The client for identity-service's internal API, authenticated with the gateway's own
 * client-credentials token (registration {@code identity}). Short timeouts: a slow identity-service
 * must not tie up the gateway.
 */
@Configuration
class IdentityClientConfig {

    @Bean
    ReactiveOAuth2AuthorizedClientManager identityAuthorizedClientManager(
            ReactiveClientRegistrationRepository registrations,
            ReactiveOAuth2AuthorizedClientService clients) {
        var manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        registrations, clients);
        manager.setAuthorizedClientProvider(
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build());
        return manager;
    }

    @Bean
    WebClient identityWebClient(
            WebClient.Builder builder,
            ReactiveOAuth2AuthorizedClientManager identityAuthorizedClientManager,
            ReactiveOAuth2AuthorizedClientService clients,
            @Value("${app.identity.url}") String identityUrl,
            @Value("${app.identity.timeout:2s}") Duration timeout) {
        var oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        identityAuthorizedClientManager);
        oauth.setDefaultClientRegistrationId("identity");
        // A 401 means identity-service refused the gateway's token (it restarted with new keys,
        // say): drop it, so the next call (SessionResolver retries once) gets a fresh one.
        oauth.setAuthorizationFailureHandler(
                new RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler(
                        (registrationId, principal, attributes) ->
                                clients.removeAuthorizedClient(
                                        registrationId, principal.getName())));
        return builder.baseUrl(identityUrl)
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create().responseTimeout(timeout)))
                .filter(oauth)
                .build();
    }
}
