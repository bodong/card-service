package xyz.pakwo.cardservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * @author sarwo.wibowo
 **/
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient riskApiRestClient(RestClient.Builder builder, @Value("${external.risk-api.base-url}") String baseUrl,
                                        @Value("${external.risk-api.timeout-ms}") long timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(timeoutMs);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
