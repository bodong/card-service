package xyz.pakwo.cardservice.integration;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import xyz.pakwo.cardservice.dto.response.ExternalRiskResponse;
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

        //this is to ease simulation, because max record in the external api is just 100
        //the idea is not about data capability, but integration with 3rd party api
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
