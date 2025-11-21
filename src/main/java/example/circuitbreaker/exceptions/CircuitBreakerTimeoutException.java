package example.circuitbreaker.exceptions;

public class CircuitBreakerTimeoutException extends CircuitBreakerException {
    public CircuitBreakerTimeoutException(String message) {
        super(message);
    }

    public CircuitBreakerTimeoutException(String message, Throwable e) {
        super(message, e);
    }
}