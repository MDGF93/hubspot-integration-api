package org.marcosdgf.hubspotintegrationapi.client;

import feign.RequestInterceptor;
import org.marcosdgf.hubspotintegrationapi.client.interceptor.HubSpotAuthRequestInterceptor;
import org.marcosdgf.hubspotintegrationapi.service.TokenStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HubSpotFeignClientConfiguration {

    @Bean
    public RequestInterceptor hubSpotAuthRequestInterceptor(final TokenStorageService tokenStorageService) {
        return new HubSpotAuthRequestInterceptor(tokenStorageService);
    }

}
