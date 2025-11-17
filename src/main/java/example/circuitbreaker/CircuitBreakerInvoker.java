package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface CircuitBreakerInvoker {

    void invokeScheduled(Runnable action, Duration interval);

    void invokeThrough(CircuitBreakerState state, Runnable action, Duration timeout);

    <T> T invokeThrough(CircuitBreakerState state, Supplier<T> func, Duration timeout);

    <T> CompletableFuture<T> invokeThroughAsync(CircuitBreakerState state, Supplier<CompletableFuture<T>> func, Duration timeout);
}