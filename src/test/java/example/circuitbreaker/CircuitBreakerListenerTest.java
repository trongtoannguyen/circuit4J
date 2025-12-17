package example.circuitbreaker;

import example.circuitbreaker.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerListenerTest {

    private final int MAX_FAILURE = 1;
    private final Duration TIMEOUT = Duration.ofMillis(100);
    private final Duration RESET_TIMEOUT = Duration.ofMillis(100);
    //@formatter:off
    private final Runnable throwAction = () -> {throw new RuntimeException();};
    private final Runnable successAction = () -> { };
    private DefaultCircuitBreaker sut;
    @Mock
    private CircuitBreakerListener listener;
    //@formatter:on

    @BeforeEach
    void setUp() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        sut = new DefaultCircuitBreaker(executor, MAX_FAILURE, TIMEOUT, RESET_TIMEOUT);
        sut.setEventListener(listener);
    }

    @Test
    void triggerEvents() throws InterruptedException {
        // force circuit to OPEN
        try {
            sut.execute(throwAction);
        } catch (Exception ignored) {
        }

        assertThrowsExactly(CircuitBreakerOpenException.class, () -> sut.execute(successAction));

        // wait for the breaker to transition to HALF-OPEN
        Thread.sleep(RESET_TIMEOUT.toMillis() + 10);

        // execute successfully while Half-Open to CLOSE
        sut.execute(successAction);

        // Verify in order
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onCircuitOpened(eq(sut));
        inOrder.verify(listener).onCircuitHalfOpened(eq(sut));
        inOrder.verify(listener).onCircuitClosed(eq(sut));

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions(listener);
    }
}