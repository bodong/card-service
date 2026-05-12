package xyz.pakwo.cardservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.pakwo.cardservice.dto.response.ExternalRiskResponse;
import xyz.pakwo.cardservice.dto.response.RiskCheckResponse;
import xyz.pakwo.cardservice.entity.CardAuthorization;
import xyz.pakwo.cardservice.entity.RiskLevel;
import xyz.pakwo.cardservice.integration.RiskApiClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sarwo.wibowo
 **/
@ExtendWith(MockitoExtension.class)
class AuthorizationRiskServiceTest {

    @Mock
    private CardAuthorizationService cardAuthorizationService;

    @Mock
    private RiskApiClient riskApiClient;

    @InjectMocks
    private AuthorizationRiskService service;

    @Test
    void performRiskCheck_shouldReturnLowRisk_whenAmountIsSmallAndExternalSignalIsLow() {
        CardAuthorization authorization = createAuthorization(1L, BigDecimal.valueOf(120.50));

        ExternalRiskResponse externalResponse = new ExternalRiskResponse(1L, 1L, "test title", "test body");

        when(cardAuthorizationService.findById(1L)).thenReturn(authorization);
        when(riskApiClient.getRiskSignal(1L)).thenReturn(externalResponse);

        RiskCheckResponse result = service.performRiskCheck(1L);

        assertThat(result.authorizationId()).isEqualTo(1L);
        assertThat(result.transactionReference()).isEqualTo("TXN-001");
        assertThat(result.riskScore()).isEqualTo(11);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.recommendation()).isEqualTo("APPROVED");
        assertThat(result.externalReference()).isEqualTo("POST-1");

        assertThat(authorization.getRiskScore()).isEqualTo(11);
        assertThat(authorization.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(authorization.getExternalRiskReference()).isEqualTo("POST-1");
        assertThat(authorization.getUpdatedAt()).isNotNull();

        verify(cardAuthorizationService).findById(1L);
        verify(riskApiClient).getRiskSignal(1L);
    }

    @Test
    void performRiskCheck_shouldReturnMediumRisk_whenAmountIsAtLeastOneThousand() {
        CardAuthorization authorization = createAuthorization(2L, BigDecimal.valueOf(1000));

        ExternalRiskResponse externalResponse = new ExternalRiskResponse(19L, 2L, "test title", "test body");

        when(cardAuthorizationService.findById(2L)).thenReturn(authorization);
        when(riskApiClient.getRiskSignal(2L)).thenReturn(externalResponse);

        RiskCheckResponse result = service.performRiskCheck(2L);

        assertThat(result.riskScore()).isEqualTo(49);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.recommendation()).isEqualTo("APPROVED");
        assertThat(result.externalReference()).isEqualTo("POST-19");

        assertThat(authorization.getRiskScore()).isEqualTo(49);
        assertThat(authorization.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void performRiskCheck_shouldReturnHighRisk_whenAmountIsAtLeastFiveThousandAndExternalSignalIsHighEnough() {
        CardAuthorization authorization = createAuthorization(99L, BigDecimal.valueOf(5000));

        ExternalRiskResponse externalResponse = new ExternalRiskResponse(99L, 99L, "test title", "test body");

        when(cardAuthorizationService.findById(99L)).thenReturn(authorization);
        when(riskApiClient.getRiskSignal(99L)).thenReturn(externalResponse);

        RiskCheckResponse result = service.performRiskCheck(99L);

        assertThat(result.riskScore()).isEqualTo(89);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.recommendation()).isEqualTo("DECLINED");
        assertThat(result.externalReference()).isEqualTo("POST-99");

        assertThat(authorization.getRiskScore()).isEqualTo(89);
        assertThat(authorization.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(authorization.getExternalRiskReference()).isEqualTo("POST-99");
    }

    @Test
    void performRiskCheck_shouldHandleNullExternalResponse() {
        CardAuthorization authorization = createAuthorization(10L, BigDecimal.valueOf(100));

        when(cardAuthorizationService.findById(10L)).thenReturn(authorization);
        when(riskApiClient.getRiskSignal(10L)).thenReturn(null);

        RiskCheckResponse result = service.performRiskCheck(10L);

        assertThat(result.riskScore()).isEqualTo(30);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.recommendation()).isEqualTo("APPROVED");
        assertThat(result.externalReference()).isEqualTo("POST-10");

        assertThat(authorization.getRiskScore()).isEqualTo(30);
        assertThat(authorization.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(authorization.getExternalRiskReference()).isEqualTo("POST-10");
    }

    @Test
    void performRiskCheck_shouldHandleExternalResponseWithNullId() {
        CardAuthorization authorization = createAuthorization(11L, BigDecimal.valueOf(100));

        ExternalRiskResponse externalResponse = new ExternalRiskResponse(null, null, "test title", "test body");

        when(cardAuthorizationService.findById(11L)).thenReturn(authorization);
        when(riskApiClient.getRiskSignal(11L)).thenReturn(externalResponse);

        RiskCheckResponse result = service.performRiskCheck(11L);

        assertThat(result.riskScore()).isEqualTo(30);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.externalReference()).isEqualTo("POST-11");

        assertThat(authorization.getRiskScore()).isEqualTo(30);
        assertThat(authorization.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(authorization.getExternalRiskReference()).isEqualTo("POST-11");
    }

    private CardAuthorization createAuthorization(Long id, BigDecimal amount) {
        CardAuthorization authorization = new CardAuthorization();
        authorization.setId(id);
        authorization.setTransactionReference("TXN-001");
        authorization.setCardNumberMasked("545454******5454");
        authorization.setCustomerId("CUST-001");
        authorization.setMerchantName("Pakwo Store");
        authorization.setAmount(amount);
        authorization.setCurrency("MYR");
        authorization.setCreatedAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());
        return authorization;
    }
}