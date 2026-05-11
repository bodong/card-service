package xyz.pakwo.cardservice.integration;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import xyz.pakwo.cardservice.dto.ExternalRiskResponse;
import xyz.pakwo.cardservice.exception.ExternalApiException;

/**
 * @author sarwo.wibowo
 **/
@Component
public class RiskApiClient {
    private final RestClient riskApiRestClient;

    public RiskApiClient(RestClient riskApiRestClient) {
        this.riskApiRestClient = riskApiRestClient;
    }

    public ExternalRiskResponse getRiskSignal(Long authorizationId) {
        long externalId = Math.max(1L, Math.min(100L, authorizationId));
        try {
            return riskApiRestClient
                    .get()
                    .uri("/posts/{id}", externalId)
                    .retrieve()
                    .body(ExternalRiskResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            throw new ExternalApiException("Risk API returned status " + statusCode.value(), ex);
        } catch (RestClientException ex) {
            throw new ExternalApiException("Risk API call failed", ex);
        }
    }
}
