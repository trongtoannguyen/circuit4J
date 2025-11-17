package example.circuitbreaker.states;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Represent the state of a Circuit Breaker.
 * Each state (Closed, Open, Half-Open) implements this interface to define its behavior.
 */
public interface CircuitBreakerState {

    /**
     * Method called when entering the state.
     */
    void enter();

    /**
     * Method called when an invocation fails.
     */
    void invocationFails();

    /**
     * Method called when an invocation succeeds.
     */
    void invocationSucceeds();

    /**
     * Executes a void action according to the state's behavior.
     *
     * @param action the action to be executed
     */
    void invoke(Runnable action);

    /**
     * Executes a function that returns a value according to the state's behavior.
     *
     * @param func the function to be executed
     * @param <T>  the return type of the function
     * @return the result of the function
     */
    <T> T invoke(Supplier<T> func);

    /**
     * Executes an asynchronous function that returns a CompletableFuture according to the state's behavior.
     *
     * @param func the asynchronous function to be executed
     * @param <T>  the return type of the CompletableFuture
     * @return a CompletableFuture representing the result of the function
     */
    <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func);
}
