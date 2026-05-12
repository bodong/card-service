package xyz.pakwo.cardservice.dto.response;

/**
 * @author sarwo.wibowo
 **/
public record ExternalRiskResponse(
        Long id,
        Long userId,
        String title,
        String body) {
}
