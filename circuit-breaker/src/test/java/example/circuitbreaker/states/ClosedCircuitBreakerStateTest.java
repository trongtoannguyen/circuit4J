package example.circuitbreaker.states;

import example.circuitbreaker.CircuitBreakerInvoker;
import example.circuitbreaker.CircuitBreakerSwitch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClosedCircuitBreakerStateTest {

    //@formatter:off
    private static final Duration TIMEOUT = Duration.ofMillis(100);
    private static final int MAX_FAILURES = 3;
    @Mock private CircuitBreakerSwitch switcher;
    @Mock private CircuitBreakerInvoker invoker;
    private ClosedCircuitBreakerState sut;
    //@formatter:on

    @BeforeEach
    void setUp() {
        sut = new ClosedCircuitBreakerState(switcher, invoker, MAX_FAILURES, TIMEOUT);
    }

    @Test
    void enter() {
        sut.invocationFails();
        assertEquals(1, sut.getFailures().get());
        sut.enter();
        assertEquals(0, sut.getFailures().get());
    }

    @Test
    void invocationFails() {
        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            sut.invocationFails();
            assertEquals(i + 1, sut.getFailures().get());
        }
    }

    @Test
    void invocationSucceeds() {
        sut.invocationFails();
        assertEquals(1, sut.getFailures().get());

        sut.invocationSucceeds();
        assertEquals(0, sut.getFailures().get());
    }

    @Nested
    class InvocationFailsBehavior {

        @Test
        void circuitTransitionAtFailureThreshold() {
            for (int i = 0; i < MAX_FAILURES - 1; i++) {
                sut.invocationFails();
            }
            verify(switcher, never()).openCircuit(any());

            // verify circuit opening after reaching max failures
            sut.invocationFails();
            verify(switcher).openCircuit(any());
            verifyNoMoreInteractions(switcher);
        }
    }

    @Nested
    class ResetBehavior {

        @Test
        void invocationSuccessResetFailureCounter() {
            sut.invocationFails();
            assertEquals(1, sut.getFailures().get());
            sut.invocationSucceeds();
            assertEquals(0, sut.getFailures().get());
        }

        @Test
        void enterResetFailureCounter() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                sut.invocationFails();
            }
            verify(switcher).openCircuit(same(sut));
            assertEquals(MAX_FAILURES, sut.getFailures().get());

            //reset failures while entering closed state
            sut.enter();
            assertEquals(0, sut.getFailures().get());
        }
    }

    @Nested
    class DelegationToInvoker {

        @Test
        void invokeDelegateToInvokerWithTimeoutFunction() {
            Runnable action = mock(Runnable.class);
            sut.invoke(action);
            verify(invoker).invokeThrough(same(sut), same(action), eq(TIMEOUT));
            verifyNoMoreInteractions(invoker);

            // supplier version
            Supplier<?> supplier = mock(Supplier.class);
            sut.invoke(supplier);
            verify(invoker).invokeThrough(same(sut), same(supplier), eq(TIMEOUT));
            verifyNoMoreInteractions(invoker);
        }

        @Test
        void invokeAsyncDelegateToInvokerWithTimeoutSupplier() {
            Supplier<CompletableFuture<String>> futureSupplier = mock(Supplier.class);
            CompletableFuture<String> expectedFuture = CompletableFuture.completedFuture("OK");
            when(invoker.invokeThroughAsync(same(sut), same(futureSupplier), eq(TIMEOUT))).thenReturn(expectedFuture);

            //call to act by sut
            CompletableFuture<String> delegatedActualFuture = sut.invokeAsync(futureSupplier);

            //compare result
            assertSame(expectedFuture, delegatedActualFuture);

            //verify interaction
            verify(invoker).invokeThroughAsync(same(sut), same(futureSupplier), eq(TIMEOUT));
            verifyNoMoreInteractions(invoker);
        }
    }
}