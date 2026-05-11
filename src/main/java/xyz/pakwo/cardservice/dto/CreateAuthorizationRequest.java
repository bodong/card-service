package xyz.pakwo.cardservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * @author sarwo.wibowo
 **/
public record CreateAuthorizationRequest(
        @NotBlank(message = "cardNumber is required")
        @Pattern(regexp = "^[0-9]{12,19}$", message = "cardNumber must contain 12 to 19 digits")
        String cardNumber,

        @NotBlank(message = "customerId is required")
        @Size(max = 64, message = "customerId must not exceed 64 characters")
        String customerId,

        @NotBlank(message = "merchantName is required")
        @Size(max = 150, message = "merchantName must not exceed 150 characters")
        String merchantName,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be ISO 4217 format, for example MYR")
        String currency
) {
}
