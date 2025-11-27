package com.example;

import example.circuitbreaker.CircuitBreaker;
import example.circuitbreaker.DefaultCircuitBreaker;
import example.circuitbreaker.exceptions.CircuitBreakerException;
import example.circuitbreaker.exceptions.CircuitBreakerExecutionException;
import example.circuitbreaker.exceptions.CircuitBreakerInterruptedException;
import example.circuitbreaker.exceptions.CircuitBreakerOpenException;
import example.circuitbreaker.exceptions.CircuitBreakerTimeoutException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
        App app = new App(executor);
        try {
            app.demo();
        } finally {
            // shutdown non-demon executor to exit the program
            shutdownExecutor(executor);
        }
    }

    private static void shutdownExecutor(ScheduledExecutorService executor) {
        System.out.println("Shutting down executor...");
        executor.shutdown();
        System.out.println("Executor shut down.");
    }

    private void demo() {
        var externalService = new ExternalService();
        var circuitBreaker = new DefaultCircuitBreaker(
                scheduledExecutorService,
                2, // max 2 failures before open the circuit
                Duration.ofMillis(10), // in 10 ms must be completed
                Duration.ofMillis(100));

        System.out.println("=== Synchronous Execution Demo ===");
        tryExecute(circuitBreaker, externalService::get);
        tryExecute(circuitBreaker, () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CircuitBreakerInterruptedException();
            }
        });
        tryExecute(circuitBreaker, externalService::get);

        System.out.println();
        System.out.println("===Asynchronous Execution Demo ====");
        tryExecuteAsync(circuitBreaker, externalService::getAsync)
                .thenRun(() -> tryExecuteAsync(circuitBreaker, () -> delayAsync(100)))
                .thenRun(() -> tryExecuteAsync(circuitBreaker, externalService::getAsync)).join();
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

    private CompletableFuture<Void> tryExecuteAsync(CircuitBreaker breaker, Supplier<CompletableFuture<Void>> supplier) {
        return breaker.executeAsync(supplier)
                .exceptionally((throwable) -> {
                    if (throwable.getCause() instanceof CircuitBreakerTimeoutException) {
                        System.out.println("CircuitBreakerTimeoutException");
                    } else if (throwable.getCause() instanceof CircuitBreakerOpenException) {
                        System.out.println("CircuitBreakerOpenException");
                    } else if (throwable.getCause() instanceof CircuitBreakerExecutionException) {
                        System.out.println("CircuitBreakerExecutionException");
                    } else if (throwable.getCause() instanceof CircuitBreakerException) {
                        System.out.println("Other CircuitBreakerException");
                    }
                    return null;
                });
    }

    private void tryExecute(DefaultCircuitBreaker breaker, Runnable action) {
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
}
