package xyz.pakwo.cardservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.pakwo.cardservice.dto.response.ExternalRiskResponse;
import xyz.pakwo.cardservice.dto.response.RiskCheckResponse;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;
import xyz.pakwo.cardservice.entity.CardAuthorization;
import xyz.pakwo.cardservice.entity.RiskLevel;
import xyz.pakwo.cardservice.integration.RiskApiClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        entity.setUpdatedAt(LocalDateTime.now());

        return new RiskCheckResponse(
                entity.getId(),
                entity.getTransactionReference(),
                riskScore,
                riskLevel,
                riskLevel == RiskLevel.HIGH ? AuthorizationStatus.DECLINED.name() : AuthorizationStatus.APPROVED.name(),
                entity.getExternalRiskReference()
        );
    }

    //this is just fake risk scoring, to ease the scenario, main idea is to ensure that able to call other 3rd party api
    private int calculateRiskScore(CardAuthorization entity, ExternalRiskResponse externalResponse) {
        int score = 10;
        BigDecimal amount = entity.getAmount();
        if (amount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            score += 20;
        }

        if (amount.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            score += 40;
        }
        if (externalResponse == null || externalResponse.id() == null) {
            score += 20;
        } else {
            score += externalResponse.id().intValue() % 20;
        }

        return Math.min(100, score);
    }

    private RiskLevel toRiskLevel(int riskScore) {
        if (riskScore >= 70) {
            return RiskLevel.HIGH;
        }
        if (riskScore >= 40) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
