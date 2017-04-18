package bekrina.whereismobile.exceptions;


public class NoCurrentUserException extends RuntimeException {
    public NoCurrentUserException(String message) {
        super(message);
    }
}
