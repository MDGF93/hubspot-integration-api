package org.marcosdgf.hubspotintegrationapi.client.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marcosdgf.hubspotintegrationapi.service.TokenStorageService;
import org.springframework.http.HttpHeaders;

@Slf4j
@RequiredArgsConstructor
public class HubSpotAuthRequestInterceptor implements RequestInterceptor {

    private final TokenStorageService tokenStorageService;

    @Override
    public void apply(final RequestTemplate template) {

        final String accessToken = tokenStorageService.getAccessToken();

        if (accessToken != null) {
            log.debug("Adding Authorization header to Feign request for target: {}", template.feignTarget().name());
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        } else {
            log.error("Cannot add Authorization header: Access Token is null. Target: {}", template.feignTarget().name());
            throw new IllegalStateException("HubSpot Access Token is not available.");
        }
    }

}
