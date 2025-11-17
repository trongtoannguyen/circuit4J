package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * DefaultCircuitBreaker is a basic implementation of the CircuitBreaker interface.
 * Calls the current state and handles state transitions.
 */
public class DefaultCircuitBreaker implements CircuitBreaker {
    private final AtomicReference<CircuitBreakerState> currentState;

    public DefaultCircuitBreaker(AtomicReference<CircuitBreakerState> currentState) {
        this.currentState = currentState;
    }

    @Override
    public void execute(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("Action must not be null");
        }
        currentState.get().invoke(action);
    }

    @Override
    public <T> T execute(Supplier<T> func) {
        if (func == null) {
            throw new IllegalArgumentException("Func must not be null");
        }
        return currentState.get().invoke(func);
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> func) {
        if (func == null) {
            throw new IllegalArgumentException("Func must not be null");
        }
        return currentState.get().invokeAsync(func);
    }
}
