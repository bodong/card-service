package xyz.pakwo.cardservice.dto;

import xyz.pakwo.cardservice.entity.AuthorizationStatus;
import xyz.pakwo.cardservice.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author sarwo.wibowo
 **/
public record CardAuthorizationResponse(
        Long id,
        String transactionReference,
        String cardNumberMasked,
        String customerId,
        String merchantName,
        BigDecimal amount,
        String currency,
        AuthorizationStatus status,
        Integer riskScore,
        RiskLevel riskLevel,
        String externalRiskReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
