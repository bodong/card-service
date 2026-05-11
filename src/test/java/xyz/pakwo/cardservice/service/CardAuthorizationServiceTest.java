package xyz.pakwo.cardservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.pakwo.cardservice.dto.CardAuthorizationResponse;
import xyz.pakwo.cardservice.dto.CreateAuthorizationRequest;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author sarwo.wibowo
 **/
@SpringBootTest
@ActiveProfiles("test")
public class CardAuthorizationServiceTest {
    @Autowired
    private CardAuthorizationService service;

    @Test
    void createShouldPersistPendingAuthorizationWithMaskedCardNumber() {
        CreateAuthorizationRequest request = new CreateAuthorizationRequest(
                "5454545454545454",
                "CUST-001",
                "Test Merchant",
                new BigDecimal("120.50"),
                "MYR"
        );

        CardAuthorizationResponse response = service.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.transactionReference()).startsWith("AUTH-");
        assertThat(response.cardNumberMasked()).isEqualTo("545454******5454");
        assertThat(response.status()).isEqualTo(AuthorizationStatus.PENDING);
    }
}
