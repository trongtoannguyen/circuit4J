package example.circuitbreaker.states;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface CircuitBreakerState {

    void enter();

    void invocationFails();

    void invocationSucceeds();

    void invoke(Runnable action);

    <T> T invoke(Supplier<T> func);

    <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func);
}
