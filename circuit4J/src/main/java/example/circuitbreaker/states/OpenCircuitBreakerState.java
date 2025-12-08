package example.circuitbreaker.states;

import example.circuitbreaker.CircuitBreakerInvoker;
import example.circuitbreaker.CircuitBreakerSwitch;
import example.circuitbreaker.exceptions.CircuitBreakerOpenException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

//todo
public class OpenCircuitBreakerState implements CircuitBreakerState {

    private final CircuitBreakerInvoker invoker;
    private final CircuitBreakerSwitch switcher;
    private final Duration resetTimeSpan;

    public OpenCircuitBreakerState(CircuitBreakerSwitch switcher, CircuitBreakerInvoker invoker, Duration resetTimeSpan) {
        this.invoker = invoker;
        this.switcher = switcher;
        this.resetTimeSpan = resetTimeSpan;
    }

    @Override
    public void enter() {
        invoker.invokeScheduled(() -> switcher.attemptToCloseCircuit(this), resetTimeSpan);
    }

    @Override
    public void invocationFails() {
    }

    @Override
    public void invocationSucceeds() {
    }

    @Override
    public void invoke(Runnable action) {
        throw new CircuitBreakerOpenException();
    }

    @Override
    public <T> T invoke(Supplier<T> func) {
        throw new CircuitBreakerOpenException();
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func) {
        throw new CircuitBreakerOpenException();
    }
}
