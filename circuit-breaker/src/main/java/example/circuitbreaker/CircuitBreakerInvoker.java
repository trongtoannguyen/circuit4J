package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The CircuitBreakerInvoker interface provides mechanisms to execute actions
 * and functions through a circuit breaker with specific timing and state constraints.
 */
public interface CircuitBreakerInvoker {

    /**
     * Schedules a periodic invocation of the specified action with the given interval.
     * The action will be executed at the specified time intervals until it is stopped
     * or canceled by the caller.
     *
     * @param action   the task to be executed periodically. Must not be null.
     * @param interval the time interval between successive executions of the action. Must not be null.
     */
    void invokeScheduled(Runnable action, Duration interval);

    /**
     * Executes the provided action through the specified CircuitBreaker state with a timeout.
     * The action will be invoked according to the rules defined by the given CircuitBreakerState.
     *
     * @param state   the current state of the CircuitBreaker through which the action is executed. Must not be null.
     * @param action  the task to be executed. Must not be null.
     * @param timeout the maximum duration for which the action execution is allowed. Must not be null.
     */
    void invokeThrough(CircuitBreakerState state, Runnable action, Duration timeout);

    /**
     * Executes the provided function through the specified CircuitBreaker state with a timeout.
     * The function will be invoked according to the rules and behavior defined by the given CircuitBreakerState.
     */
    <T> T invokeThrough(CircuitBreakerState state, Supplier<T> func, Duration timeout);

    /**
     * Executes the provided asynchronous function through a given CircuitBreaker state with a specified timeout.
     * The function is expected to return a CompletableFuture and will be invoked
     * according to the rules and behavior defined by the provided CircuitBreakerState.
     */
    <T> CompletableFuture<T> invokeThroughAsync(CircuitBreakerState state, Supplier<CompletableFuture<T>> func, Duration timeout);
}