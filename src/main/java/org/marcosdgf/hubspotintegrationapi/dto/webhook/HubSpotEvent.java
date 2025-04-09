package org.marcosdgf.hubspotintegrationapi.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HubSpotEvent {

    @JsonProperty("objectId")
    private Long objectId;

    @JsonProperty("subscriptionType")
    private String subscriptionType;

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("portalId")
    private Integer portalId;

    @JsonProperty("occurredAt")
    private Long occurredAt;

    @JsonProperty("propertyName")
    private String propertyName;

    @JsonProperty("propertyValue")
    private String propertyValue;

}
