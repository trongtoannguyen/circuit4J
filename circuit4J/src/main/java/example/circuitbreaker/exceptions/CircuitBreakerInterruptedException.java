package example.circuitbreaker.exceptions;

/**
 * Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or
 * during the activity.
 */
public class CircuitBreakerInterruptedException extends CircuitBreakerException {
    private static final String msg = "CircuitBreakerInterruptedException";

    public CircuitBreakerInterruptedException() {
        super(msg);
    }

    public CircuitBreakerInterruptedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
