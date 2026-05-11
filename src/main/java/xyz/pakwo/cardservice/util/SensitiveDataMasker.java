package xyz.pakwo.cardservice.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * @author sarwo.wibowo
 **/
@Component
public class SensitiveDataMasker {
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\b(\\d{6})\\d{2,9}(\\d{4})\\b");

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

    public String maskText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return CARD_NUMBER_PATTERN.matcher(value).replaceAll("$1******$2");
    }

    public String maskJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }

        return payload
                .replaceAll("(\"cardNumber\"\\s*:\\s*\")([^\"]+)(\")", "$1****MASKED****$3")
                .replaceAll("(\"cvv\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3")
                .replaceAll("(\"pin\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3")
                .replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3")
                .replaceAll("(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
    }
}
