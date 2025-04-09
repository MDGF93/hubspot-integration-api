# HubSpot Integration API

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=fff)](https://www.docker.com/products/docker-desktop/)


A Spring Boot application demonstrating integration with the HubSpot API. This project implements:

1.  **OAuth 2.0 Authorization Code Flow:** Handles authentication with HubSpot to obtain access and refresh tokens.
2.  **HubSpot API Client (Contacts):** Uses Spring Cloud OpenFeign to interact with the HubSpot CRM API, specifically for creating contacts.
3.  **Webhook Receiver:** Listens for HubSpot webhook events (e.g., contact creation) and validates incoming requests using HubSpot's V3 signature verification.
4.  **Automatic Token Management:** Stores tokens (in-memory) and automatically handles refreshing expired access tokens using the refresh token.
5.  **API Documentation:** Provides API documentation using Swagger.

## Features

*   **OAuth 2.0:**
    *   Endpoint to generate the HubSpot authorization URL (`/oauth/authorize`).
    *   Callback endpoint (`/oauth/callback`) to handle the authorization code exchange for tokens.
    *   Uses `HubSpotOAuthClient` (Feign) for token exchange and refresh.
*   **Contact Creation:**
    *   `POST /contacts` endpoint to create a new contact in HubSpot.
    *   Uses `HubSpotCrmClient` (Feign) to communicate with the HubSpot API.
    *   Requires authentication (Basic Auth provided by Spring Security).
    *   Validates request payload (`ContactCreateRequest`).
*   **Webhook Handling:**
    *   `POST /webhooks/contacts` endpoint to receive webhook notifications from HubSpot.
    *   Implements HubSpot Signature Verification V3 (`X-HubSpot-Signature-v3`) for security.
    *   Validates the request timestamp (`X-HubSpot-Request-Timestamp`).
    *   Logs received contact creation events.
    *   When you create a new contact you'll see an log like this on your Docker container:

`hubspot-integration-api-app  | 2025-04-09T21:36:28.100Z  INFO 1 --- [hubspot-integration-api] [nio-8080-exec-6] o.m.h.controller.WebhookController       : Webhook Recebido! Timestamp: 1744234589203, Signature: 9v3TysmKvTOyEjdz6RVf1Vds5TbUT7lKlG33dagIl7A=
`
*   **Token Storage & Refresh:**
    *   `InMemoryTokenStorageService` stores the latest access and refresh tokens in memory.
    *   Automatically attempts to refresh the access token using the refresh token when it's expired or close to expiry before making API calls.
*   **Security:**
    *   Uses Spring Security for basic authentication on protected endpoints (`/contacts`).
    *   Public endpoints (`/`, `/oauth/*`, `/webhooks/*`, `/swagger-ui/**`, `/v3/api-docs/**`) are accessible without authentication.
    *   Secure webhook validation.
*   **API Documentation:**
    *   Accessible at `/swagger-ui.html` (You'll be automatically redirected to it if you access the root URL).

## Technologies Used

*   **Java 21**
*   Spring Boot 3.2.6
*   Spring Web (MVC)
*   Spring Security
*   Spring Cloud OpenFeign
*   Lombok
*   Jackson (JSON Processing)
*   Swagger
*   H2 Database
*   **Gradle** (Build Tool)
*   JUnit 5 & Mockito (Testing)
*   Docker

## Getting Started

### Prerequisites

To build and run this application we'll be using Docker and Docker Compose, you will need the following:

1.  **Docker and Docker Compose:**
    * Install [Docker Engine](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/). They are required to build the application image and orchestrate the containers.
        * *Note: You do **not** need Java (JDK/JRE) or Gradle installed directly on your host machine, as they are managed within the Docker containers.*

2.  **HubSpot Developer Account:**
    * Create or access your account at [developer.hubspot.com](https://developers.hubspot.com/).

3.  **HubSpot App:**
    * Create an app within your HubSpot developer account.
    * Note down the **Client ID** and **Client Secret**.
        * On your Developer account go to Apps > *Click on your HubSpot app* > Basic Info > *Click on Auth tab*.
    * Ensure the app has the required **Scopes** configured (e.g., `crm.objects.contacts.write`, `crm.objects.contacts.read`, `oauth`).
    * Configure the **Redirect URI** in your HubSpot app settings. This URI must match the public address that HubSpot will use to return after OAuth2 authentication. When using the Ngrok setup provided in `docker-compose.yml`, Ngrok will create a public tunnel to your local application. You will need to start `docker-compose up --build -V` check the public URL provided by Ngrok (e.g., `https://<unique-hash>.ngrok-free.app`) that'll appear on the container's log, and then configure the full Redirect URI in HubSpot (`https://<unique-hash>.ngrok-free.app/oauth/callback`).


4.  **Ngrok Account (Optional, but required for the default `docker-compose.yml` setup):**
    * Create an account at [ngrok.com](https://ngrok.com/).
    * Get your **Authtoken** from the Ngrok dashboard. It is needed to authenticate the Ngrok service in `docker-compose.yml`.

5.  **`.env` Configuration File:**
    * Create a file named `.env` in the project root directory (the same directory as `docker-compose.yml`).
    * This file will contain the necessary environment variables to configure the Docker Compose services. Add the following variables to the `.env` file, replacing the example values with your own:

    ```dotenv
    # HubSpot Credentials
    HUBSPOT_CLIENT_ID=your_hubspot_client_id
    HUBSPOT_CLIENT_SECRET=your_hubspot_client_secret
    
    # Ngrok Authtoken
    NGROK_AUTHTOKEN=your_ngrok_authtoken

    # App Credentials (Optional - Defaults: hubspot/hubspot)
    # APP_USER=desired_username
    # APP_PASSWORD=desired_password
    ```

After setting up these prerequisites, you will be ready to build and run the application using Docker Compose commands.

TL;DR:
0. Make sure you have set up correctly your .env file
1. Type `docker-compose up --build -V` to completely set up our Dockerized Environment;
2. Once our Docker container is up and running check the container's logs and look for a line like this:
   `SCRIPT: Found ngrok HTTPS URL: https://<unique-hash>.ngrok-free.app`;
3. Use this URL to access the application, log in (default login and password are both `hubspot`);
4. Copy and paste this same URL `https://<unique-hash>.ngrok-free.app` on Redirect URLs on your HubSpot app (On your Developer account go to Apps > Click on your HubSpot app > Basic Info > Click on Auth tab > Scroll down to Redirect URLs and click on `+ Add redirect URL` and paste your URL adding `/oauth/callback` to the end of it, like so: `https://<unique-hash>.ngrok-free.app/oauth/callback`);
    1. Make sure your HubSpot app contains these 3 required scope: `oauth`, `crm.objects.contacts.write`
       and `crm.objects.contacts.read`;
5. Create a webhook (it's on the same page we got access our App's info, just under **Features**) with the subscription type of `contact.creation`;
6. Use the same URL we got from Ngrok to set up the Webhook's Target URL: `https://<unique-hash>.ngrok-free.app/webhooks/contacts`;
7. Go to the Root URL of the application (`https://<unique-hash>.ngrok-free.app`) and log-in;
8. Under the OAuth endpoints go to /oauth/authorize and click **Try it out** and then **Execute**;
9. Copy and paste the authorizationUrl into a new tab, and select an account to link our app to;
10. You should be redirected to a page that says ``OAuth bem-sucedido! Token recebido e processado via Feign.``
11. Now go back to the Root URL of our application and under **Contacts** create a new contact, here's an example of a valid request body to this endpoint:
```json
{
  "email": "johndoe@gmail.com",
  "firstname": "John",
  "lastname": "Doe",
  "phone": "123456789",
  "website": "www.johndoe.com"
}
```
12. Now after you register this new contact you should see a message like this pop up on our Docker container log: `2025-04-08T21:36:28.100Z INFO 1 --- [hubspot-integration-api] [nio-8080-exec-6] o.m.h.controller.WebhookController : Webhook Recebido! Timestamp: 1744234589203, Signature: 9v3TysmKvTOyEjdz6RVf1Vds5TbUT7lKlG33dagIl7A=`

### Configuration

Set the following as environment variables using a .env file like so:

```.env
# HubSpot app keys
HUBSPOT_CLIENT_ID=
HUBSPOT_CLIENT_SECRET=

#Tunnel Token auth key
NGROK_AUTHTOKEN=

#User credentials (Default values are for username and password are both `hubspot`)
APP_USER=
APP_PASSWORD=
```

## Future improvements

### I. Observability (Metrics, Dashboards, Logging, Tracing)
1.**Metrics Collection (Spring Boot Actuator & Micrometer):**

* **Enable Actuator Endpoints**: Expose endpoints like ``/actuator/health``, `/actuator/info`, `/actuator/prometheus`.
* **Instrument Key Operations**: Use Micrometer to track:

    * HTTP request latency and counts per endpoint (`/contacts`, `/oauth/callback`,` /webhooks/contacts`).

    * Feign Client call latency and success/error counts (`HubSpotCrmClient`, `HubSpotOAuthClient`).

    * OAuth token refresh success/failure rates.

    * Webhook processing counts, success/error rates, and validation failures.

    * Business-specific metrics (e.g., number of contacts created).
* **Dashboards (Prometheus & Grafana)**:
    * **Setup Prometheus**: Configure Prometheus to scrape the `/actuator/prometheus` endpoint.
    * **Create Grafana Dashboards**: Build dashboards in Grafana to visualize the metrics collected by Prometheus. Create panels for:
        * Request rates and latencies (overall and per endpoint).
        * Error rates (HTTP 5xx, Feign errors, webhook validation errors).
        * HubSpot API interaction performance.

### II. Error Handling & Resilience

1. **Global Exception Handler (`@ControllerAdvice`)**:
    * **Centralize Handling**: Create a class annotated with `@ControllerAdvice` and `@ExceptionHandler` methods to handle specific exceptions (e.g., FeignException, ValidationException, custom exceptions) consistently across all controllers.
    * **Standardized Error Response**: Define a standard JSON structure for error responses (e.g., `{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Invalid email format", "path": "/contacts", "traceId": "..." }`).
2. **More Specific Custom Exceptions**:
    * Define exceptions like `TokenRefreshFailedException`, `WebhookValidationException`, `HubSpotApiException` to provide clearer context than generic exceptions.
3. **Asynchronous Processing**: Decouple webhook reception from processing. The `/webhooks/contacts` endpoint should ideally just validate the signature, acknowledge receipt (return `200 OK` quickly), and place the event onto a queue (e.g., `RabbitMQ`, `Kafka`).


### III. Testing

1. **Integration Tests**:
    * Test the interaction between controllers, services, and potentially mocked Feign clients.
    * Test the OAuth callback flow more thoroughly, mocking the `HubSpotOAuthClient`.
    * Test webhook validation and basic processing logic within the Spring context.

2. **Enhanced Unit Tests**:

    * Increase coverage for `WebhookController`, specifically `isValidSignatureV3`, covering edge cases (e.g., timestamp variations, signature mismatches, different request methods/URLs).
    * Add tests for edge cases in `InMemoryTokenStorageService` (e.g., what happens if refresh token is null during refresh attempt).

3. **Test Coverage Reporting (JaCoCo)**:
    * Integrate JaCoCo to measure test coverage and identify untested code paths. Aim for a reasonable coverage target for critical components.

### IV. Security Enhancements
1. **Secrets Management**: Move `HUBSPOT_CLIENT_SECRET` and `HUBSPOT_CLIENT_ID` out of environment variables/properties files. Use a dedicated secrets manager (e.g., AWS Secrets Manager) and fetch secrets at runtime.
2. **Rate Limiting**: Implement rate limiting on endpoints, especially `/oauth/authorize` and `/webhooks/**`, to prevent abuse.

### V. Feature Enhancements & HubSpot Integration
1. **Handle More Webhook Events**: Implement handlers for other subscriptionTypes (e.g., `contact.propertyChange`, `contact.deletion`, `deal.creation`).

### VI. Code Quality & Maintainability
1. **Code Style Enforcement**: Use tools like **Checkstyle** to enforce a consistent code style across the project.
2. **Continuous Integration with Jenkins**:
    * **Validation**: During the build, Jenkins compiles the code. If there are syntax errors in your configuration properties class or if the necessary dependencies are missing, the build will fail, providing immediate feedback.
    * **Testing Support**: Jenkins runs unit and integration tests. If there's an issue, the tests will fail in the Jenkins pipeline.
    * **Automation**: Jenkins automates the execution of these code style checks during the build process.