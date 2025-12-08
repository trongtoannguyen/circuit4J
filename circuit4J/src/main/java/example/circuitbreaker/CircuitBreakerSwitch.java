package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;

/**
 * This interface defines methods for transitioning a circuit breaker between various states.
 * It is responsible for handling the operations of opening, closing, and attempting
 * to close the circuit based on the current state.
 */
public interface CircuitBreakerSwitch {

    /**
     * Method to open the circuit breaker
     *
     * @param from the state from which the circuit is being opened
     */
    void openCircuit(CircuitBreakerState from);

    /**
     * Method to attempt to close the circuit breaker.
     * Called when in Half-Open state and conditions to close are met.
     */
    void attemptToCloseCircuit(CircuitBreakerState from);

    /**
     * Method to close the circuit breaker
     */
    void closeCircuit(CircuitBreakerState from);
}
