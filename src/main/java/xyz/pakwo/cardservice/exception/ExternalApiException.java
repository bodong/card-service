package xyz.pakwo.cardservice.exception;

/**
 * @author sarwo.wibowo
 **/
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
