package example.circuitbreaker;

import example.circuitbreaker.exceptions.CircuitBreakerException;
import example.circuitbreaker.exceptions.CircuitBreakerTimeoutException;
import example.circuitbreaker.states.CircuitBreakerState;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class DefaultCircuitBreakerInvoker implements CircuitBreakerInvoker {

    private final ScheduledExecutorService scheduledExecutor;
    private volatile ScheduledFuture<?> timerHandle;

    public DefaultCircuitBreakerInvoker() {
        this.scheduledExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    public DefaultCircuitBreakerInvoker(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = Objects.requireNonNull(scheduledExecutor);
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
            throw new CircuitBreakerException("Invocation failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CircuitBreakerException("Invocation interrupted", e);
        } finally {
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
        return timeOutAfter(future, timeout);
    }

    private <T> CompletableFuture<T> timeOutAfter(CompletableFuture<T> future, Duration timeout) {
        Objects.requireNonNull(future);

        //treat as "no timeout"
        if (timeout.isZero() || timeout.isNegative()) {
            return future;
        }

        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        ScheduledFuture<Boolean> timeoutHandle = scheduledExecutor.schedule(
                () -> timeoutFuture.completeExceptionally(new CircuitBreakerTimeoutException("Invocation time out")),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        // cancel timeout task when the future completes first
        future.whenComplete((t, throwable) -> timeoutHandle.cancel(false));

        return future.applyToEither(timeoutFuture, t -> t); // when either completes, return its result
    }

    //helper method to cancel any existing timer
    private void cancelTimerIfNeeded() {
        if (timerHandle != null && !timerHandle.isDone()) {
            timerHandle.cancel(true);
        }
    }
}
