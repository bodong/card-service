package xyz.pakwo.cardservice.exception;

/**
 * @author sarwo.wibowo
 **/
public class DuplicateTransactionReferenceException extends RuntimeException {
    public DuplicateTransactionReferenceException(String message) {
        super(message);
    }
}
