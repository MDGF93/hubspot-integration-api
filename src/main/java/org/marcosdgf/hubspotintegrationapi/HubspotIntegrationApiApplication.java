package org.marcosdgf.hubspotintegrationapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class HubspotIntegrationApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(HubspotIntegrationApiApplication.class, args);
    }

}
