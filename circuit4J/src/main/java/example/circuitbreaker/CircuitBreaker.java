package example.circuitbreaker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Defines the contract for a Circuit Breaker.
 * It provides methods to execute actions and functions with circuit breaker behavior.
 *
 * @author Toan Nguyen
 */
public interface CircuitBreaker {
    void execute(Runnable action);

    <T> T execute(Supplier<T> func);

    <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> func);
}
