package org.marcosdgf.hubspotintegrationapi.client;

import org.marcosdgf.hubspotintegrationapi.dto.response.HubSpotTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "hubspot-oauth", url = "${hubspot.oauth.tokenUri}")
public interface HubSpotOAuthClient {

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    HubSpotTokenResponse exchangeCodeForToken(
            @RequestParam("grant_type") final String grantType,
            @RequestParam("client_id") final String clientId,
            @RequestParam("client_secret") final String clientSecret,
            @RequestParam("redirect_uri") final String redirectUri,
            @RequestParam("code") final String code);

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    HubSpotTokenResponse refreshToken(
            @RequestParam("grant_type") final String grantType,
            @RequestParam("client_id") final String clientId,
            @RequestParam("client_secret") final String clientSecret,
            @RequestParam("refresh_token") final String refreshToken);

}
