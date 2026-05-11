package xyz.pakwo.cardservice.exception;

/**
 * @author sarwo.wibowo
 **/
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
