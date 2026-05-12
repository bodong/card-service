package xyz.pakwo.cardservice.util;

import org.springframework.stereotype.Component;
import xyz.pakwo.cardservice.config.CardServiceProperties;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sarwo.wibowo
 **/
@Component
public class SensitiveDataMasker {

    private static final String MASKED_VALUE = "****MASKED****";
    private final List<String> jsonFieldsToMask;

    public SensitiveDataMasker(CardServiceProperties properties) {
        this.jsonFieldsToMask = properties.masking() != null
                && properties.masking().jsonFields() != null
                ? properties.masking().jsonFields()
                : List.of();
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return cardNumber;
        }

        String digitsOnly = cardNumber.replaceAll("\\D", "");

        if (digitsOnly.length() < 10) {
            return "****";
        }

        String firstSix = digitsOnly.substring(0, 6);
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);

        return firstSix + "******" + lastFour;
    }

    public String maskJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }

        String maskedPayload = payload;

        for (String fieldName : jsonFieldsToMask) {
            maskedPayload = maskJsonField(maskedPayload, fieldName);
        }

        return maskedPayload;
    }

    private String maskJsonField(String payload, String fieldName) {
        String regex = "(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\")([^\"]+)(\")";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(payload);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String originalValue = matcher.group(2);

            String maskedValue = "cardNumber".equals(fieldName)
                    ? maskCardNumber(originalValue)
                    : MASKED_VALUE;

            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + maskedValue + matcher.group(3)));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}