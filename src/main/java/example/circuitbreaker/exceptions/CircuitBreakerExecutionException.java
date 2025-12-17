package example.circuitbreaker.exceptions;

/**
 * Exception thrown when attempting to retrieve the result of a task that aborted by throwing an exception.
 */
public class CircuitBreakerExecutionException extends CircuitBreakerException {
    public CircuitBreakerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
