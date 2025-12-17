package example.circuitbreaker.states;

import example.circuitbreaker.CircuitBreakerInvoker;
import example.circuitbreaker.CircuitBreakerSwitch;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * {@code ClosedCircuitBreakerState} represents the normal state where the circuit is closed and requests flow through.
 * It tracks failures and opens the circuit when the failure threshold is reached.
 */
public class ClosedCircuitBreakerState implements CircuitBreakerState {

    // threshold before opening the circuit
    private final int maxFailures;
    private final CircuitBreakerSwitch switcher;
    private final CircuitBreakerInvoker invoker;

    // max time for each invocation
    private final Duration invocationTimeout;

    // thread-safe failure counter
    private final AtomicInteger failures = new AtomicInteger(0);

    public AtomicInteger getFailures() {
        return failures;
    }

    public ClosedCircuitBreakerState(CircuitBreakerSwitch switcher, CircuitBreakerInvoker invoker, int maxFailures, Duration invocationTimeout) {
        this.maxFailures = maxFailures;
        this.switcher = switcher;
        this.invoker = invoker;
        this.invocationTimeout = invocationTimeout;
    }

    /**
     * Called when entering the CLOSED state.
     * Resets the failure counter to 0.
     */
    @Override
    public void enter() {
        resetFailures();
    }

    /**
     * This method is called when an invocation fails.
     * Atomically increments the failure count and opens the circuit if reach {@code maxFailures}.
     */
    @Override
    public void invocationFails() {
        if (failures.incrementAndGet() >= maxFailures) {
            switcher.openCircuit(this);
        }
    }

    /**
     * Called when an invocation succeeds.
     * Resets the failure counter to 0.
     */
    @Override
    public void invocationSucceeds() {
        resetFailures();
    }

    @Override
    public void invoke(Runnable action) {
        invoker.invokeThrough(this, action, invocationTimeout);
    }

    @Override
    public <T> T invoke(Supplier<T> func) {
        return invoker.invokeThrough(this, func, invocationTimeout);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func) {
        return invoker.invokeThroughAsync(this, func, invocationTimeout);
    }

    private void resetFailures() {
        failures.set(0);
    }
}
