package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;
import example.circuitbreaker.states.ClosedCircuitBreakerState;
import example.circuitbreaker.states.HalfOpenCircuitBreakerState;
import example.circuitbreaker.states.OpenCircuitBreakerState;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * DefaultCircuitBreaker is a basic implementation of the CircuitBreaker interface.
 * Calls the current state and handles state transitions.
 */
public class DefaultCircuitBreaker implements CircuitBreaker, CircuitBreakerSwitch {
    private final CircuitBreakerState closedState;
    private final CircuitBreakerState openState;
    private final CircuitBreakerState halfOpenState;
    private final CircuitBreakerListener eventListener;
    private final AtomicReference<CircuitBreakerState> currentState;

    public DefaultCircuitBreaker(int maxFailureThreshold, CircuitBreakerSwitch switcher, CircuitBreakerListener eventListener,
                                 Duration resetTimeOut, AtomicReference<CircuitBreakerState> currentState) {
        CircuitBreakerInvoker invoker = new DefaultCircuitBreakerInvoker();

        this.closedState = new ClosedCircuitBreakerState(maxFailureThreshold, switcher, invoker, resetTimeOut);
        this.openState = new OpenCircuitBreakerState();
        this.halfOpenState = new HalfOpenCircuitBreakerState();
        this.eventListener = eventListener;
        this.currentState = currentState;
    }

    public CircuitBreakerState getOpenState() {
        return openState;
    }

    public CircuitBreakerState getHalfOpenState() {
        return halfOpenState;
    }

    public CircuitBreakerState getClosedState() {
        return closedState;
    }

    public CircuitBreakerListener getEventListener() {
        return eventListener;
    }

    public AtomicReference<CircuitBreakerState> getCurrentState() {
        return currentState;
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

    @Override
    public void closeCircuit(CircuitBreakerState from) {
        boolean isTransitioned = tryTransitionState(from, closedState);
        if (isTransitioned && eventListener != null) {
            eventListener.onCircuitClosed(this);
        }
    }

    @Override
    public void openCircuit(CircuitBreakerState from) {
        boolean isTransitioned = tryTransitionState(from, openState);
        if (isTransitioned && eventListener != null) {
            eventListener.onCircuitOpened(this);
        }
    }

    @Override
    public void attemptToCloseCircuit(CircuitBreakerState from) {
        boolean isTransitioned = tryTransitionState(from, halfOpenState);
        if (isTransitioned && eventListener != null) {
            eventListener.onCircuitHalfOpened(this);
        }
    }

    private boolean tryTransitionState(CircuitBreakerState from, CircuitBreakerState to) {
        if (currentState.compareAndSet(from, to)) {
            to.enter();
            return true;
        }
        return false;
    }
}
