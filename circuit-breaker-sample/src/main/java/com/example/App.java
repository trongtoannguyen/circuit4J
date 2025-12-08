package com.example;

import example.circuitbreaker.CircuitBreaker;
import example.circuitbreaker.DefaultCircuitBreaker;
import example.circuitbreaker.exceptions.CircuitBreakerInterruptedException;
import example.circuitbreaker.exceptions.CircuitBreakerOpenException;
import example.circuitbreaker.exceptions.CircuitBreakerTimeoutException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Hello world!
 */
public class App {
    private final ScheduledExecutorService scheduledExecutorService;

    public App(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        try {
            App app = new App(executor);
            app.demo();
        } finally {
            // shutdown non-demon executor to exit the program
            shutdownExecutor(executor);
        }
    }

    private static void shutdownExecutor(ScheduledExecutorService executor) {
        System.out.println("Shutting down executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Task did not complete, forcing shutdown...");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Executor shut down.");
    }

    private static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CircuitBreakerInterruptedException();
        }
    }

    private static void tryExecute(CircuitBreaker breaker, Runnable action) {
        try {
            breaker.execute(action);
        } catch (CircuitBreakerTimeoutException e) {
            System.out.println("CircuitBreakerTimeoutException");
        } catch (CircuitBreakerOpenException e) {
            System.out.println("CircuitBreakerOpenException");
        } catch (Exception e) {
            System.out.println("Exception");
        }
    }

    private static CompletableFuture<Void> tryExecuteAsync(CircuitBreaker breaker, Supplier<CompletableFuture<Void>> supplier) {
        try {
            return breaker.executeAsync(supplier);
        } catch (CircuitBreakerOpenException e) {
            System.out.println("CircuitBreakerOpenException");
            return CompletableFuture.failedFuture(e);
        } catch (CircuitBreakerTimeoutException e) {
            System.out.println("CircuitBreakerTimeoutException");
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            System.out.println("Exception");
            return CompletableFuture.failedFuture(e);
        }
    }

    private void demo() {
        ExternalService externalService = new ExternalService();
        DefaultCircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
                scheduledExecutorService,
                2, // max 2 failures before open the circuit
                Duration.ofMillis(10), // in 10 ms must be completed
                Duration.ofMillis(100)); // after 100 ms attempt to close the circuit

        System.out.println("=== Synchronous Execution Demo ===");
        tryExecute(circuitBreaker, externalService::getError);
        tryExecute(circuitBreaker, () -> delay(100));
        tryExecute(circuitBreaker, externalService::getError);

        System.out.println();
        System.out.println("===Asynchronous Execution Demo ====");
        CompletableFuture<Void> future = tryExecuteAsync(circuitBreaker, externalService::getErrorAsync)
                .whenCompleteAsync((unused, throwable) -> tryExecuteAsync(circuitBreaker, () -> delayAsync(100)), scheduledExecutorService)
                .whenCompleteAsync((unused, throwable) -> tryExecuteAsync(circuitBreaker, externalService::getErrorAsync), scheduledExecutorService);
        try {
            future.join();
        } catch (Exception e) {
            // handle exception if needed
        }
    }

    private CompletableFuture<Void> delayAsync(long delayMillis) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CircuitBreakerInterruptedException();
            }
        }, scheduledExecutorService);
    }
}
