package xyz.pakwo.cardservice.dto;

import xyz.pakwo.cardservice.entity.RiskLevel;

/**
 * @author sarwo.wibowo
 **/
public record RiskCheckResponse(
        Long authorizationId,
        String transactionReference,
        Integer riskScore,
        RiskLevel riskLevel,
        String recommendation,
        String externalReference
) {
}
