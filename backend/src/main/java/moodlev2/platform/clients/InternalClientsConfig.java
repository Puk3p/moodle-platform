package moodlev2.platform.clients;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * HTTP clients for the other services' internal APIs. Every call carries a service token from
 * identity-service's token endpoint (client-credentials grant, registration {@code identity}),
 * cached until it expires; the service-based client manager works outside any request too (Kafka
 * listeners). Timeouts are short: a slow service must not hold a request thread.
 */
@Configuration
public class InternalClientsConfig {

    static final String REGISTRATION = "identity";

    /**
     * Whom the cached token belongs to: this service, never the user (or service) whose request is
     * being served. Otherwise every caller would get a token fetched and cached of their own, and a
     * caller without an email claim (a service token) would have no name to cache it under.
     */
    private static final Authentication SELF =
            UsernamePasswordAuthenticationToken.unauthenticated("academic-service", null);

    @Bean
    OAuth2AuthorizedClientManager internalAuthorizedClientManager(
            ClientRegistrationRepository registrations, OAuth2AuthorizedClientService clients) {
        var manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    RestClient identityRestClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager internalAuthorizedClientManager,
            @Value("${app.services.identity.url}") String url,
            @Value("${app.services.identity.timeout:2s}") Duration timeout) {
        return client(builder, internalAuthorizedClientManager, url, timeout);
    }

    @Bean
    RestClient progressRestClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager internalAuthorizedClientManager,
            @Value("${app.services.progress.url}") String url,
            @Value("${app.services.progress.timeout:1500ms}") Duration timeout) {
        return client(builder, internalAuthorizedClientManager, url, timeout);
    }

    @Bean
    RestClient assessmentRestClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager internalAuthorizedClientManager,
            @Value("${app.services.assessment.url}") String url,
            @Value("${app.services.assessment.timeout:1500ms}") Duration timeout) {
        return client(builder, internalAuthorizedClientManager, url, timeout);
    }

    private static RestClient client(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager manager,
            String url,
            Duration timeout) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> REGISTRATION);
        interceptor.setPrincipalResolver(request -> SELF);
        // The builder is a shared prototype; each client gets its own copy.
        return builder.clone()
                .baseUrl(url)
                .requestFactory(
                        ClientHttpRequestFactoryBuilder.detect()
                                .build(
                                        ClientHttpRequestFactorySettings.defaults()
                                                .withConnectTimeout(timeout)
                                                .withReadTimeout(timeout)))
                .requestInterceptor(interceptor)
                .build();
    }
}
