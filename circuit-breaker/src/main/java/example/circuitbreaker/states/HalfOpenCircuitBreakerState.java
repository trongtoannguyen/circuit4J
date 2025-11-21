package example.circuitbreaker.states;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HalfOpenCircuitBreakerState implements CircuitBreakerState {
    @Override
    public void enter() {

    }

    @Override
    public void invocationFails() {

    }

    @Override
    public void invocationSucceeds() {

    }

    @Override
    public void invoke(Runnable action) {

    }

    @Override
    public <T> T invoke(Supplier<T> func) {
        return null;
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func) {
        return null;
    }
}
