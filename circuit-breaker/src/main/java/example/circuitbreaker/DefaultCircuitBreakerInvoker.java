package example.circuitbreaker;

import example.circuitbreaker.exceptions.CircuitBreakerExecutionException;
import example.circuitbreaker.exceptions.CircuitBreakerInterruptedException;
import example.circuitbreaker.exceptions.CircuitBreakerTimeoutException;
import example.circuitbreaker.states.CircuitBreakerState;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Default implementation of the {@link CircuitBreakerInvoker} interface. This class provides
 * mechanisms for executing actions and functions through a circuit breaker while adhering
 * to timing constraints and handling circuit breaker states.
 * <p>
 * This invoker leverages a {@link ScheduledExecutorService} to manage timed and scheduled
 * tasks, allowing for synchronous and asynchronous executions with configurable timeouts.
 */
public class DefaultCircuitBreakerInvoker implements CircuitBreakerInvoker {

    private final ScheduledExecutorService scheduledExecutor;
    private volatile ScheduledFuture<?> timerHandle;

    public DefaultCircuitBreakerInvoker(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutor = Objects.requireNonNull(scheduledExecutorService);
    }

    @Override
    public void invokeScheduled(Runnable action, Duration interval) {
        Objects.requireNonNull(action);
        cancelTimerIfNeeded(); // Cancel any existing timer
        timerHandle = scheduledExecutor.schedule(action, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void invokeThrough(CircuitBreakerState state, Runnable action, Duration timeout) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(action);
        try {
            invoke(action, timeout);
        } catch (Exception e) {
            state.invocationFails();
            throw e;
        }

        state.invocationSucceeds();
    }

    // Delegate to the Supplier-based invoke method
    private void invoke(Runnable action, Duration timeout) {
        invoke(() -> {
            action.run();
            return null;
        }, timeout);
    }

    @Override
    public <T> T invokeThrough(CircuitBreakerState state, Supplier<T> func, Duration timeout) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(func);
        T result;
        try {
            result = invoke(func, timeout);
        } catch (Exception e) {
            state.invocationFails();
            throw e;
        }

        state.invocationSucceeds();
        return result;
    }

    private <T> T invoke(Supplier<T> func, Duration timeout) {
        Objects.requireNonNull(func);
        Future<T> tFuture = scheduledExecutor.submit(func::get);
        try {
            return tFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new CircuitBreakerTimeoutException("Invocation time out", e.getCause());
        } catch (ExecutionException e) {
            throw new CircuitBreakerExecutionException("Invocation execution failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CircuitBreakerInterruptedException("Invocation interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            // todo: review method boolean param
            tFuture.cancel(true);
        }
    }


    @Override
    public <T> CompletableFuture<T> invokeThroughAsync(CircuitBreakerState state, Supplier<CompletableFuture<T>> func,
                                                       Duration timeout) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(func);
        CompletableFuture<T> future;
        try {
            future = invokeAsync(func, timeout);
        } catch (Exception e) {
            state.invocationFails();
            throw e;
        }

        return future.whenComplete((t, throwable) -> {
            if (Objects.isNull(throwable)) {
                state.invocationSucceeds();
            } else {
                state.invocationFails();
            }
        });
    }

    private <T> CompletableFuture<T> invokeAsync(Supplier<CompletableFuture<T>> func, Duration timeout) {
        Objects.requireNonNull(func);
        CompletableFuture<T> future = func.get();

        // apply timeout
        return CompletableFutureUtil.timeOutAfter(future, timeout);
    }

    //helper method to cancel any existing timer
    private void cancelTimerIfNeeded() {
        if (timerHandle != null && !timerHandle.isDone()) {
            timerHandle.cancel(true);
        }
    }
}
