package example.circuitbreaker;

import example.circuitbreaker.exceptions.CircuitBreakerOpenException;
import example.circuitbreaker.exceptions.CircuitBreakerTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CircuitBreakerTest {
    private static final int MAX_FAILURES = 3;
    private static final Duration INVOKE_TIMEOUT = Duration.ofMillis(100);
    private static final Duration RESET_TIMEOUT = Duration.ofMillis(100);
    private CircuitBreaker sut;
    private ScheduledExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newScheduledThreadPool(1);
        sut = new DefaultCircuitBreaker(executor, MAX_FAILURES, INVOKE_TIMEOUT, RESET_TIMEOUT);
    }

    @Nested
    class ExecuteActionTest {
        //@formatter:off
        private final Runnable anyAction = () -> {};
        private final Runnable throwAction = () -> {throw new RuntimeException();};
        private final Runnable timeoutAction = () -> {
            try {
                Thread.sleep(INVOKE_TIMEOUT.toMillis() + 100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        //@formatter:on

        @Test
        void successfulExecution() {
            for (int i = 0; i < 100; i++) {
                sut.execute(anyAction);
            }
        }

        @Test
        void timeouts() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                assertThrows(CircuitBreakerTimeoutException.class, () -> sut.execute(timeoutAction));
            }
            assertThrows(CircuitBreakerOpenException.class, () -> sut.execute(anyAction));
        }

        @Test
        void failures() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                assertThrows(Exception.class, () -> sut.execute(throwAction));
            }
            assertThrows(CircuitBreakerOpenException.class, () -> sut.execute(anyAction));
        }

        @Test
        void resetAfterTimeout() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                assertThrows(Exception.class, () -> sut.execute(throwAction));
            }

            try {
                Thread.sleep(RESET_TIMEOUT.toMillis() + 100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertDoesNotThrow(() -> sut.execute(anyAction));
        }
    }

    @Nested
    class ExecuteFunctionTest {
        //@formatter:off
        private final Supplier<?> anyFunc = Object::new;
        private final Supplier<?> timeoutFunc = () -> {
            try {
                Thread.sleep(INVOKE_TIMEOUT.toMillis() + 100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return new Object();
        };
        private final Supplier<?> throwFunc = () -> {throw new RuntimeException();};
        //@formatter:on

        @Test
        void successfulExecution() {
            for (int i = 0; i < 100; i++) {
                sut.execute(anyFunc);
            }
        }

        @Test
        void timeouts() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                assertThrows(CircuitBreakerTimeoutException.class, () -> sut.execute(timeoutFunc));
            }
            assertThrows(CircuitBreakerOpenException.class, () -> sut.execute(anyFunc));
        }

        @Test
        void failures() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                assertThrows(Exception.class, () -> sut.execute(throwFunc));
            }
            assertThrows(CircuitBreakerOpenException.class, () -> sut.execute(anyFunc));
        }

        @Test
        void resetAfterTimeout() {
            for (int i = 0; i < MAX_FAILURES; i++) {
                assertThrows(Exception.class, () -> sut.execute(throwFunc));
            }

            try {
                Thread.sleep(RESET_TIMEOUT.toMillis() + 100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertDoesNotThrow(() -> sut.execute(anyFunc));
        }
    }

}
