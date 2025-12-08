package example.circuitbreaker.exceptions;

public class CircuitBreakerException extends RuntimeException {

    private static final String msg = "Circuit Breaker Exception";

    public CircuitBreakerException() {
        super(msg);
    }

    public CircuitBreakerException(String message) {
        super(message);
    }

    public CircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
}
