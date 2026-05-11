package xyz.pakwo.cardservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.pakwo.cardservice.dto.ExternalRiskResponse;
import xyz.pakwo.cardservice.dto.RiskCheckResponse;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;
import xyz.pakwo.cardservice.entity.CardAuthorization;
import xyz.pakwo.cardservice.entity.RiskLevel;
import xyz.pakwo.cardservice.integration.RiskApiClient;

/**
 * @author sarwo.wibowo
 **/
@Service
public class AuthorizationRiskService {
    private final CardAuthorizationService cardAuthorizationService;
    private final RiskApiClient riskApiClient;

    public AuthorizationRiskService(CardAuthorizationService cardAuthorizationService,
                                    RiskApiClient riskApiClient) {
        this.cardAuthorizationService = cardAuthorizationService;
        this.riskApiClient = riskApiClient;
    }

    @Transactional
    public RiskCheckResponse performRiskCheck(Long authorizationId) {
        CardAuthorization entity = cardAuthorizationService.findById(authorizationId);
        ExternalRiskResponse externalResponse = riskApiClient.getRiskSignal(authorizationId);
        int riskScore = calculateRiskScore(entity, externalResponse);
        RiskLevel riskLevel = toRiskLevel(riskScore);

        Long externalId = externalResponse != null && externalResponse.id() != null
                ? externalResponse.id()
                : authorizationId;

        entity.setRiskScore(riskScore);
        entity.setRiskLevel(riskLevel);
        entity.setExternalRiskReference("POST-" + externalId);
        entity.setStatus(riskLevel == RiskLevel.HIGH ? AuthorizationStatus.DECLINED : AuthorizationStatus.APPROVED);

        return new RiskCheckResponse(
                entity.getId(),
                entity.getTransactionReference(),
                riskScore,
                riskLevel,
                riskLevel == RiskLevel.HIGH ? AuthorizationStatus.DECLINED.name() : AuthorizationStatus.APPROVED.name(),
                entity.getExternalRiskReference()
        );
    }

    private int calculateRiskScore(CardAuthorization entity, ExternalRiskResponse externalResponse) {
        int amountFactor = entity.getAmount().intValue() % 50;
        int externalFactor = externalResponse == null || externalResponse.id() == null ? 30 : externalResponse.id().intValue() % 50;
        return Math.min(100, 10 + amountFactor + externalFactor);
    }

    private RiskLevel toRiskLevel(int riskScore) {
        if (riskScore >= 75) {
            return RiskLevel.HIGH;
        }
        if (riskScore >= 40) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
