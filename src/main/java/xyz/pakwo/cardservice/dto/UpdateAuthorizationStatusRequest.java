package xyz.pakwo.cardservice.dto;

import jakarta.validation.constraints.NotNull;
import xyz.pakwo.cardservice.entity.AuthorizationStatus;

/**
 * @author sarwo.wibowo
 **/
public record UpdateAuthorizationStatusRequest(
        @NotNull(message = "status is required")
        AuthorizationStatus status) {
}
