package com.cloud.auth.service;

import com.cloud.common.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OAuth2ClientCredentialsService {

    private static final ThreadLocal<Boolean> GETTING_TOKEN = ThreadLocal.withInitial(() -> false);

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceManager;

    @Value("${spring.security.oauth2.client.registration.client-service.client-id:client-service}")
    private String clientId;

    public OAuth2ClientCredentialsService(
            AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceManager) {
        this.authorizedClientServiceManager = authorizedClientServiceManager;
    }

    @DistributedLock(
            key = "'oauth2:token:' + #clientId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL,
            failMessage = "Failed to acquire OAuth2 token lock"
    )
    public String getInternalApiToken() {
        if (Boolean.TRUE.equals(GETTING_TOKEN.get())) {
            log.debug("Detected recursive token acquisition, skip current request");
            return null;
        }

        try {
            GETTING_TOKEN.set(true);

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("client-service")
                    .principal(clientId)
                    .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientServiceManager.authorize(authorizeRequest);
            if (authorizedClient == null) {
                log.error("Failed to get OAuth2 authorized client");
                return null;
            }

            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            if (accessToken == null) {
                log.error("Authorized client returned null access token");
                return null;
            }

            return accessToken.getTokenValue();
        } catch (Exception e) {
            log.error("Failed to get internal API token", e);
            return null;
        } finally {
            GETTING_TOKEN.remove();
        }
    }
}
