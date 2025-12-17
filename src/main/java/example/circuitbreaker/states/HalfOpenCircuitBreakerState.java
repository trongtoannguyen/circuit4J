package example.circuitbreaker.states;

import example.circuitbreaker.CircuitBreakerInvoker;
import example.circuitbreaker.CircuitBreakerSwitch;
import example.circuitbreaker.exceptions.CircuitBreakerOpenException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

//todo
public class HalfOpenCircuitBreakerState implements CircuitBreakerState {
    private final int notInvoking = 0;
    private final int invoking = 1;
    private final CircuitBreakerInvoker invoker;
    private final Duration timeout;
    private AtomicInteger isBeingInvoked = new AtomicInteger(notInvoking);
    private final CircuitBreakerSwitch switcher;

    public int getNotInvoking() {
        return notInvoking;
    }

    public int getInvoking() {
        return invoking;
    }

    public CircuitBreakerInvoker getInvoker() {
        return invoker;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public AtomicInteger isBeingInvoked() {
        return isBeingInvoked;
    }

    public CircuitBreakerSwitch getSwitcher() {
        return switcher;
    }

    /**
     * Constructs a new instance of HalfOpenCircuitBreakerState.
     * This state represents the "half-open" condition in a circuit breaker,
     * where a limited number of requests are allowed to execute to test
     * if the circuit can safely transition back to the "closed" state.
     *
     * @param switcher the CircuitBreakerSwitch to handle transitions between states
     * @param invoker  the CircuitBreakerInvoker to execute actions with specific timing and constraints
     * @param timeout  the timeout duration for requests while in the half-open state
     */
    public HalfOpenCircuitBreakerState(CircuitBreakerSwitch switcher, CircuitBreakerInvoker invoker, Duration timeout) {
        this.switcher = switcher;
        this.invoker = invoker;
        this.timeout = timeout;
    }

    @Override
    public void enter() {
        isBeingInvoked = new AtomicInteger(notInvoking);
    }

    @Override
    public void invocationFails() {
        switcher.openCircuit(this);
    }

    @Override
    public void invocationSucceeds() {
        switcher.closeCircuit(this);
    }

    @Override
    public void invoke(Runnable action) {
        // if already being invoked e.g., by another thread, do not allow further invocations
        if (isBeingInvoked.compareAndSet(notInvoking, invoking)) {
            invoker.invokeThrough(this, action, timeout);
        } else {
            throw new CircuitBreakerOpenException();
        }
    }

    @Override
    public <T> T invoke(Supplier<T> func) {
        if (isBeingInvoked.compareAndSet(notInvoking, invoking)) {
            return invoker.invokeThrough(this, func, timeout);
        } else {
            throw new CircuitBreakerOpenException();
        }
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func) {
        if (isBeingInvoked.compareAndSet(notInvoking, invoking)) {
            return invoker.invokeThroughAsync(this, func, timeout);
        } else {
            throw new CircuitBreakerOpenException();
        }
    }
}
