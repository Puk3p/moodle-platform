package moodlev2.messaging.core;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * The HTTP client for core. Tokens come from core's token endpoint (client-credentials grant,
 * registration {@code core}) and are cached until they expire; this works outside any request too
 * (Kafka listeners), hence the service-based client manager.
 */
@Configuration
public class CoreClientConfig {

    static final String REGISTRATION = "core";

    @Bean
    OAuth2AuthorizedClientManager coreAuthorizedClientManager(
            ClientRegistrationRepository registrations, OAuth2AuthorizedClientService clients) {
        var manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    RestClient coreRestClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager coreAuthorizedClientManager,
            @Value("${app.core.url}") String coreUrl,
            @Value("${app.core.timeout:2s}") Duration timeout) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(coreAuthorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> REGISTRATION);
        return builder.baseUrl(coreUrl)
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
