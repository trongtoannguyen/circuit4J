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
    private final Duration timeout;

    // thread-safe failure counter
    private final AtomicInteger failures = new AtomicInteger(0);

    public ClosedCircuitBreakerState(int maxFailures, CircuitBreakerSwitch switcher, CircuitBreakerInvoker invoker, Duration timeout) {
        this.maxFailures = maxFailures;
        this.switcher = switcher;
        this.invoker = invoker;
        this.timeout = timeout;
    }

    /**
     * Called when entering the CLOSED state.
     * Resets the failure counter to 0.
     */
    @Override
    public void enter() {
        failures.set(0);
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
        failures.set(0);
    }

    @Override
    public void invoke(Runnable action) {
        invoker.invokeThrough(this, action, timeout);
    }

    @Override
    public <T> T invoke(Supplier<T> func) {
        return invoker.invokeThrough(this, func, timeout);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func) {
        return invoker.invokeThroughAsync(this, func, timeout);
    }
}
