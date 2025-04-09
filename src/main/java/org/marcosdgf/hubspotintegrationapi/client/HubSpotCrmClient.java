package org.marcosdgf.hubspotintegrationapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "hubspot-crm",
        url = "${hubspot.api.baseUri}",
        configuration = HubSpotFeignClientConfiguration.class)
public interface HubSpotCrmClient {

    @PostMapping(
            value = "${hubspot.api.contacts.path}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    String createContact(@RequestBody final Map<String, Object> contactData);

}
