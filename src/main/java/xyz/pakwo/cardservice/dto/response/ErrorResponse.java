package xyz.pakwo.cardservice.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author sarwo.wibowo
 **/
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors) {
}
