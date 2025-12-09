package example.circuitbreaker;

import example.circuitbreaker.exceptions.CircuitBreakerOpenException;
import example.circuitbreaker.exceptions.CircuitBreakerTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class để kiểm thử concurrent access vào async methods của CircuitBreaker.
 * Đảm bảo thread-safety khi nhiều threads truy cập đồng thời.
 */
@DisplayName("Concurrent Async Execution Tests")
public class ConcurrentAsyncExecutionTest {

    private static final int MAX_FAILURES = 3;
    private static final Duration INVOKE_TIMEOUT = Duration.ofMillis(100);
    private static final Duration RESET_TIMEOUT = Duration.ofMillis(200);

    private CircuitBreaker circuitBreaker;
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() {
        scheduledExecutor = Executors.newScheduledThreadPool(4);
        testExecutor = Executors.newFixedThreadPool(20);
        circuitBreaker = new DefaultCircuitBreaker(
                scheduledExecutor,
                MAX_FAILURES,
                INVOKE_TIMEOUT,
                RESET_TIMEOUT
        );
    }

    @AfterEach
    void tearDown() {
        shutdownExecutor(scheduledExecutor);
        shutdownExecutor(testExecutor);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Nested
    @DisplayName("Multiple Concurrent Successful Async Calls")
    class ConcurrentSuccessfulCalls {

        @Test
        @DisplayName("100 concurrent async calls should all succeed")
        void multipleConcurrentSuccessfulAsyncCalls() throws InterruptedException {
            int numberOfThreads = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Tạo 100 threads cùng gọi executeAsync
            for (int i = 0; i < numberOfThreads; i++) {
                int finalI = i;
                testExecutor.submit(() -> {
                    try {
                        // Chờ signal để tất cả threads start cùng lúc
                        startLatch.await();

                        Supplier<CompletableFuture<String>> asyncTask = () ->
                                CompletableFuture.supplyAsync(() -> {
                                    // Simulate some work
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                    System.out.println("Task done " + finalI);
                                    return "Success";
                                }, scheduledExecutor);
                        circuitBreaker.executeAsync(asyncTask)
                                .thenAccept(result -> successCount.incrementAndGet())
                                .exceptionally(ex -> {
                                    failureCount.incrementAndGet();
                                    return null;
                                })
                                .whenComplete((unused, throwable) -> completionLatch.countDown())
                                .join();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        completionLatch.countDown();
                    }
                });
            }

            // Start tất cả threads cùng lúc
            startLatch.countDown();

            // Chờ tất cả threads hoàn thành
            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "All threads should complete within timeout");

            // Verify: Tất cả calls đều thành công
            assertEquals(numberOfThreads, successCount.get(),
                    "All async calls should succeed");
            assertEquals(0, failureCount.get(),
                    "No failures should occur");
        }
    }

    @Nested
    @DisplayName("Race Condition Tests")
    class RaceConditionTests {

        @Test
        @DisplayName("Concurrent state transitions should be thread-safe")
        void concurrentStateTransitions() throws InterruptedException {
            int numberOfThreads = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Tạo nhiều threads cùng trigger failures để force state transition
            for (int i = 0; i < numberOfThreads; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        startLatch.await();

                        // Mỗi thread gọi một task sẽ fail
                        Supplier<CompletableFuture<Void>> failingTask = () ->
                                CompletableFuture.failedFuture(new RuntimeException("Intentional failure"));

                        try {
                            circuitBreaker.executeAsync(failingTask).join();
                        } catch (Exception e) {
                            // Expected
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completionLatch.countDown();
                    }
                }, testExecutor);

                futures.add(future);
            }

            startLatch.countDown();
            for (CompletableFuture<Void> future : futures) {
                future.join();
            }
            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "All threads should complete");

            // Verify: Circuit breaker vẫn hoạt động đúng sau concurrent access
            // Circuit nên ở trạng thái OPEN
            Supplier<CompletableFuture<String>> task = () -> CompletableFuture.completedFuture("Test");
            assertThrows(CircuitBreakerOpenException.class, () -> circuitBreaker.executeAsync(task).join());
        }
    }

    @Nested
    @DisplayName("High Load Stress Tests")
    class HighLoadStressTests {

        @Test
        @DisplayName("1000 concurrent async calls under high load")
        void highLoadConcurrentAsyncCalls() throws InterruptedException {
            int numberOfCalls = 1000;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(numberOfCalls);
            AtomicInteger completedCount = new AtomicInteger(0);

            for (int i = 0; i < numberOfCalls; i++) {
                testExecutor.submit(() -> {
                    try {
                        startLatch.await();

                        Supplier<CompletableFuture<Integer>> asyncTask = () ->
                                CompletableFuture.supplyAsync(() -> {
                                    // Simulate light work
                                    return (int) (Math.random() * 100);
                                }, scheduledExecutor);

                        circuitBreaker.executeAsync(asyncTask)
                                .whenComplete((result, ex) -> completedCount.incrementAndGet())
                                .join();

                    } catch (Exception e) {
                        completedCount.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
            assertTrue(completed, "All calls should complete within timeout");
            assertEquals(numberOfCalls, completedCount.get(),
                    "All calls should be processed");
        }
    }

    @Nested
    @DisplayName("Circuit Recovery Tests")
    class CircuitRecoveryTests {

        @Test
        @DisplayName("Circuit should recover after reset timeout with concurrent access")
        void circuitRecoveryWithConcurrentAccess() throws InterruptedException {
            // Phase 1: Trip the circuit
            for (int i = 0; i < MAX_FAILURES; i++) {
                try {
                    Supplier<CompletableFuture<Void>> failingTask = () ->
                            CompletableFuture.failedFuture(new RuntimeException("Fail"));
                    circuitBreaker.executeAsync(failingTask).join();
                } catch (Exception e) {
                    // Expected
                }
            }

            // Verify circuit is open
            Supplier<CompletableFuture<String>> task = () -> CompletableFuture.completedFuture("Test");
            assertThrows(CircuitBreakerOpenException.class, () -> circuitBreaker.executeAsync(task).join());

            // Phase 2: Wait for reset timeout
            Thread.sleep(RESET_TIMEOUT.toMillis() + 100);

            // Phase 3: Multiple threads try to access concurrently after reset
            int numberOfThreads = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < numberOfThreads; i++) {
                testExecutor.submit(() -> {
                    try {
                        startLatch.await();

                        Supplier<CompletableFuture<String>> successTask = () -> CompletableFuture.completedFuture("Success");

                        circuitBreaker.executeAsync(successTask)
                                .thenAccept(result -> successCount.incrementAndGet())
                                .join();

                    } catch (Exception e) {
                        // Some may fail if circuit is in half-open and only allows one
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
            assertTrue(completed, "All threads should complete");

            // At least one should succeed (half-open allows testing)
            assertTrue(successCount.get() > 0,
                    "At least one call should succeed after reset");
        }
    }

    @Nested
    @DisplayName("Timeout Handling Under Concurrent Load")
    class TimeoutHandlingTests {

        @Test
        @DisplayName("Concurrent timeout scenarios should be handled correctly")
        void concurrentTimeoutHandling() throws InterruptedException {
            int numberOfThreads = 30;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
            AtomicInteger timeoutCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < numberOfThreads; i++) {
                final int threadIndex = i;
                testExecutor.submit(() -> {
                    try {
                        startLatch.await();

                        Supplier<CompletableFuture<String>> asyncTask;

                        // 50% will timeout
                        if (threadIndex % 2 == 0) {
                            asyncTask = () -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    Thread.sleep(INVOKE_TIMEOUT.toMillis() + 100);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return "Timeout";
                            }, scheduledExecutor);
                        } else {
                            asyncTask = () -> CompletableFuture.completedFuture("Success");
                        }

                        circuitBreaker.executeAsync(asyncTask)
                                .thenAccept(result -> successCount.incrementAndGet())
                                .exceptionally(ex -> {
                                    if (ex.getCause() instanceof CircuitBreakerTimeoutException) {
                                        timeoutCount.incrementAndGet();
                                    }
                                    return null;
                                })
                                .join();

                    } catch (Exception e) {
                        // Handle exceptions
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = completionLatch.await(15, TimeUnit.SECONDS);
            assertTrue(completed, "All threads should complete");

            System.out.println("Timeouts detected: " + timeoutCount.get());
            System.out.println("Successful calls: " + successCount.get());

            assertTrue(timeoutCount.get() > 0,
                    "Should detect timeout exceptions");
        }
    }
}
