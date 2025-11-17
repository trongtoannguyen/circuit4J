package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DefaultCircuitBreakerInvoker implements CircuitBreakerInvoker {
    @Override
    public void invokeScheduled(Runnable action, Duration interval) {

    }

    @Override
    public void invokeThrough(CircuitBreakerState state, Runnable action, Duration timeout) {

    }

    @Override
    public <T> T invokeThrough(CircuitBreakerState state, Supplier<T> func, Duration timeout) {
        return null;
    }

    @Override
    public <T> CompletableFuture<T> invokeThroughAsync(CircuitBreakerState state, Supplier<CompletableFuture<T>> func, Duration timeout) {
        return null;
    }
}
