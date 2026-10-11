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
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * The client for core's internal API, authenticated with the gateway's own client-credentials token
 * (registration {@code core}). Short timeouts: a slow core must not tie up the gateway.
 */
@Configuration
class CoreClientConfig {

    @Bean
    ReactiveOAuth2AuthorizedClientManager coreAuthorizedClientManager(
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
    WebClient coreWebClient(
            WebClient.Builder builder,
            ReactiveOAuth2AuthorizedClientManager coreAuthorizedClientManager,
            @Value("${app.core.url}") String coreUrl,
            @Value("${app.core.timeout:2s}") Duration timeout) {
        var oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(coreAuthorizedClientManager);
        oauth.setDefaultClientRegistrationId("core");
        return builder.baseUrl(coreUrl)
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create().responseTimeout(timeout)))
                .filter(oauth)
                .build();
    }
}
