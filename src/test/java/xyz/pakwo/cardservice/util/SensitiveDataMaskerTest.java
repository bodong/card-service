package xyz.pakwo.cardservice.util;

import org.junit.jupiter.api.Test;
import xyz.pakwo.cardservice.config.CardServiceProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sarwo.wibowo
 **/
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker(
            new CardServiceProperties(
                    new CardServiceProperties.Pagination(50),
                    new CardServiceProperties.Masking(
                            List.of("cardNumber", "cvv", "pin", "password", "token")
                    )
            )
    );

    @Test
    void maskCardNumber_shouldKeepFirstSixAndLastFourDigits() {
        String result = masker.maskCardNumber("5454545454545454");
        assertThat(result).isEqualTo("545454******5454");
    }

    @Test
    void maskCardNumber_shouldReturnOriginalValue_whenInputIsNull() {
        assertThat(masker.maskCardNumber(null)).isNull();
    }

    @Test
    void maskCardNumber_shouldReturnOriginalValue_whenInputIsBlank() {
        assertThat(masker.maskCardNumber(" ")).isEqualTo(" ");
    }

    @Test
    void maskCardNumber_shouldMaskShortDigitValue() {
        String result = masker.maskCardNumber("123456789");
        assertThat(result).isEqualTo("****");
    }

    @Test
    void maskJson_shouldMaskConfiguredFields() {
        String payload = """
                {
                  "cardNumber": "5454545454545454",
                  "cvv": "123",
                  "pin": "123456",
                  "password": "secret",
                  "token": "abc-token",
                  "customerId": "CUST-001"
                }
                """;

        String result = masker.maskJson(payload);

        assertThat(result).contains("\"cardNumber\": \"****MASKED****\"");
        assertThat(result).contains("\"cvv\": \"****MASKED****\"");
        assertThat(result).contains("\"pin\": \"****MASKED****\"");
        assertThat(result).contains("\"password\": \"****MASKED****\"");
        assertThat(result).contains("\"token\": \"****MASKED****\"");
        assertThat(result).contains("\"customerId\": \"CUST-001\"");
    }

    @Test
    void maskJson_shouldReturnOriginalPayload_whenPayloadIsNull() {
        assertThat(masker.maskJson(null)).isNull();
    }

    @Test
    void maskJson_shouldReturnOriginalPayload_whenPayloadIsBlank() {
        assertThat(masker.maskJson(" ")).isEqualTo(" ");
    }
}