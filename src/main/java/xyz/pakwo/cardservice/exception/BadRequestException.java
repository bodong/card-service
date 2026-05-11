package xyz.pakwo.cardservice.exception;

/**
 * @author sarwo.wibowo
 **/
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
