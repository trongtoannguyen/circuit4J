package example.circuitbreaker;

import example.circuitbreaker.states.CircuitBreakerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerInvokerTest {
    private static final Duration TIMEOUT = Duration.ofMillis(100);
    private CircuitBreakerInvoker sut;
    private ScheduledExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(1);
        sut = new DefaultCircuitBreakerInvoker(executor);
    }

    @Nested
    class InvokeScheduledTest {

        @Test
        void scheduledActionActionSuccessfulInvocation() throws InterruptedException {
            CountDownLatch signalLatch = new CountDownLatch(1);
            CountDownLatch reset = new CountDownLatch(1);

            sut.invokeScheduled(() -> {
                signalLatch.countDown();
                reset.countDown();
            }, Duration.ZERO);

            assertTrue(reset.await(500, TimeUnit.MILLISECONDS), "Task should have been executed within 500ms");
            assertEquals(0, signalLatch.getCount());
        }

        @Nested
        class InvokeThroughTest {

            @Mock
            private CircuitBreakerState state;

            @Test
            void actionFailureInvocation() {
                assertThrows(Exception.class, () -> {
                    sut.invokeThrough(state, () -> {
                        throw new RuntimeException();
                    }, TIMEOUT);
                });

                verify(state, times(1)).invocationFails();
                verify(state, times(0)).invocationSucceeds();
            }

            @Test
            void actionSuccessfulInvocation() {
                assertDoesNotThrow(() -> sut.invokeThrough(state,
                        () -> System.out.println("Any action run successfully"),
                        TIMEOUT));

                verify(state).invocationSucceeds();
                verify(state, never()).invocationFails();
            }

            @Test
            void supplierFailureInvocation() {
                Supplier<Object> supplier = () -> {
                    throw new RuntimeException();
                };
                assertThrows(Exception.class, () ->
                        sut.invokeThrough(state, supplier, TIMEOUT));
                verify(state).invocationFails();
                verify(state, never()).invocationSucceeds();
            }

            @Test
            void supplierSuccessfulInvocation() {
                Supplier<?> supplier = Object::new;
                assertNotNull(
                        assertDoesNotThrow(() -> sut.invokeThrough(state, supplier, TIMEOUT)));
                verify(state).invocationSucceeds();
                verify(state, never()).invocationFails();
            }
        }

        @Nested
        class InvokeThroughAsyncTest {

            @Mock
            private CircuitBreakerState state;

            @Test
            void actionFailureInvocation() {
                Supplier<CompletableFuture<Void>> func = () -> {
                    throw new RuntimeException();
                };
                assertThrows(Exception.class, () -> sut.invokeThroughAsync(state, func, TIMEOUT));
                verify(state).invocationFails();
                verify(state, never()).invocationSucceeds();
            }

            @Test
            void actionSuccessfulInvocation() {
                Supplier<CompletableFuture<Void>> func = () ->
                        CompletableFuture.completedFuture(null);
                assertDoesNotThrow(() -> {
                    sut.invokeThroughAsync(state, func, TIMEOUT).get(100, TimeUnit.MILLISECONDS);
                });
                verify(state).invocationSucceeds();
                verify(state, never()).invocationFails();
            }

            @Test
            void supplierFailureInvocation() {
                Supplier<CompletableFuture<Object>> supplier = () -> {
                    throw new RuntimeException();
                };
                assertNotNull(
                        assertThrows(Exception.class, () -> sut.invokeThroughAsync(state, supplier, TIMEOUT)));
                verify(state).invocationFails();
                verify(state, never()).invocationSucceeds();
            }

            @Test
            void supplierSuccessfulInvocation() {
                Object expectedResult = new Object();
                Supplier<CompletableFuture<Object>> supplier = () -> CompletableFuture.completedFuture(expectedResult);
                Object result = assertDoesNotThrow(() -> sut.invokeThroughAsync(state, supplier, TIMEOUT)
                        .get(100, TimeUnit.MILLISECONDS));
                assertEquals(expectedResult, result);
                verify(state).invocationSucceeds();
                verify(state, never()).invocationFails();
            }
        }
    }
}