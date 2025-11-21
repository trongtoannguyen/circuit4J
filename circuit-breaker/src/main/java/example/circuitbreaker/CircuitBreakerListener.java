package example.circuitbreaker;

/**
 * Introduces event listener when a breaker is transitioned between states.
 *
 * @author Toan Nguyen
 */
public interface CircuitBreakerListener {
    /**
     * Raise when the circuit is closed.
     *
     * @param breaker the CircuitBreaker instance that transitioned to the closed state
     */
    void onCircuitClosed(CircuitBreaker breaker);

    /**
     * Invoked when the circuit breaker transitions to an open state.
     *
     * @param breaker the CircuitBreaker instance that transitioned to the open state
     */
    void onCircuitOpened(CircuitBreaker breaker);

    /**
     * Invoked when the circuit breaker transitions to the Half-Open state.
     * This state indicates that the circuit breaker is allowing a limited number
     * of trial requests to determine if the underlying issue has been resolved.
     *
     * @param breaker the CircuitBreaker instance that transitioned to the Half-Open state
     */
    void onCircuitHalfOpened(CircuitBreaker breaker);
}
