package example.circuitbreaker.states;

import example.circuitbreaker.CircuitBreakerInvoker;
import example.circuitbreaker.CircuitBreakerSwitch;
import example.circuitbreaker.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class HalfOpenCircuitBreakerStateTest {

    //@formatter:off
    private static final Duration TIMEOUT = Duration.ofMillis(100);
    private HalfOpenCircuitBreakerState sut;
    @Mock private CircuitBreakerInvoker invoker;
    @Mock private CircuitBreakerSwitch switcher;
    //@formatter:on

    @BeforeEach
    void setUp() {
        sut = new HalfOpenCircuitBreakerState(switcher, invoker, TIMEOUT);
    }

    @Nested
    class StateInvokeTest {

        @Test
        void invokeFirstActionOnly() {
            Runnable action = mock(Runnable.class);
            InOrder inOrder = Mockito.inOrder(invoker);

            //#1: action is invoked here
            sut.invoke(action);
            assertEquals(1, sut.isBeingInvoked().get());

            //#2: later action is rejected
            assertThrows(CircuitBreakerOpenException.class, () -> sut.invoke(action));

            //verify in order
            //#1
            inOrder.verify(invoker).invokeThrough(same(sut), same(action), eq(TIMEOUT));

            //#2: no more invocations by throwing CircuitBreakerOpenException
            inOrder.verify(invoker, never()).invokeThrough(same(sut), same(action), any());
            verifyNoMoreInteractions(invoker);
        }

        @Test
        void invokeFirstSupplierOnly() {
            Supplier<String> func = mock(Supplier.class);
            InOrder inOrder = Mockito.inOrder(invoker);

            //#1: func is invoked here
            sut.invoke(func);
            assertEquals(1, sut.isBeingInvoked().get());

            //#2: later func is rejected
            assertThrows(CircuitBreakerOpenException.class, () -> sut.invoke(func));

            //verify in order
            //#1
            inOrder.verify(invoker).invokeThrough(same(sut), same(func), eq(TIMEOUT));

            //#2: no more invocations by throwing CircuitBreakerOpenException
            inOrder.verify(invoker, never()).invokeThrough(same(sut), same(func), any());
            verifyNoMoreInteractions(invoker);
        }
    }

    @Nested
    class StateInvokeAsyncTest {

        @Test
        void invokeFirstActionOnlyAsync() {
            // define objects
            Supplier<CompletableFuture<Boolean>> futureSupplier = () -> CompletableFuture.completedFuture(false);
            InOrder inOrder = Mockito.inOrder(invoker);

            // verify action result in order
            sut.invokeAsync(futureSupplier);
            inOrder.verify(invoker).invokeThroughAsync(same(sut), same(futureSupplier), eq(TIMEOUT));
            inOrder.verifyNoMoreInteractions();

            assertThrows(CircuitBreakerOpenException.class, () -> sut.invokeAsync(futureSupplier));
            inOrder.verify(invoker, never()).invokeThroughAsync(same(sut), same(futureSupplier), eq(TIMEOUT));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        void invokeFirstFunctionOnlyAsync() {
            Object expectedResult = new Object();
            Supplier<CompletableFuture<Object>> futureSupplier = () -> CompletableFuture.completedFuture(expectedResult);
            InOrder inOrder = Mockito.inOrder(invoker);

            sut.invokeAsync(futureSupplier);
            inOrder.verify(invoker).invokeThroughAsync(same(sut), same(futureSupplier), eq(TIMEOUT));
            inOrder.verifyNoMoreInteractions();

            //refuse latter action
            assertThrows(CircuitBreakerOpenException.class, () -> sut.invokeAsync(futureSupplier));
            inOrder.verify(invoker, never()).invokeThroughAsync(same(sut), same(futureSupplier), any());
            inOrder.verifyNoMoreInteractions();
        }
    }
}