package example.circuitbreaker.exceptions;

public class CircuitBreakerOpenException extends CircuitBreakerException {

    public CircuitBreakerOpenException() {
        super("Circuit Breaker is open. Execution is not allowed.");
    }
}
