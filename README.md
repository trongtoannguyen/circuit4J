# Circuit Breaker Java

[![Maven Central](https://img.shields.io/maven-central/v/io.github.trongtoannguyen/circuit4J)](https://search.maven.org/artifact/io.github.trongtoannguyen/circuit4J)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

This module is a Java implementation of The Circuit Breaker Pattern using the State Pattern to manage the behavior of
the breaker.

## Features

- ✅ Thread-safe state management (Closed, Open, Half-Open)
- ✅ Synchronous and asynchronous execution support
- ✅ Configurable failure thresholds and timeouts
- ✅ Automatic circuit reset with configurable intervals
- ✅ Event listener interface for monitoring
- ✅ Zero runtime dependencies
- ✅ Java 11+ compatible

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.trongtoannguyen</groupId>
    <artifactId>circuit4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

implementation 'io.github.trongtoannguyen:circuit4J:1.0.0'

<!-- ## Quick Start
## Documentation

[Link to detailed docs] -->

## License

Apache License 2.0 - see [LICENSE](LICENSE) file

## Structure

The module is structured into the following packages:

```markdown
circuit-breaker
├── src
│ ├── main
│ │ └── java
│ │ └── example.circuitbreaker
│ │ ├── exceptions
│ │ │ ├── CircuitBreakerException
│ │ │ └── ...
│ │ │
│ │ ├── states
│ │ │ ├── CircuitBreakerState           (I)
│ │ │ ├── ClosedCircuitBreakerState
│ │ │ ├── HalfOpenCircuitBreakerState
│ │ │ └── OpenCircuitBreakerState
│ │ │
│ │ ├── CircuitBreaker                  (I)
│ │ ├── CircuitBreakerInvoker           (I)
│ │ ├── CircuitBreakerListener          (I)
│ │ ├── CircuitBreakerSwitch            (I)
│ │ ├── DefaultCircuitBreaker
│ │ └── DefaultCircuitBreakerInvoker
│ └── test
├── pom.xml

(I): interface
```

## Key Elements

### 1. Core Components

- `CircuitBreaker`: the public-facing contract, defines behaviors of the circuit breaker. Clients
  interact with this interface primarily to wrap their unsafe function calls.
- `DefaultCircuitBreaker`: the main implementation of the `CircuitBreaker` and `CircuitBreakerSwitch` interfaces and
  acts as the Context in the State pattern:
    - It maintains a reference to the current state (Open, Closed, Half-Open)
    - Delegates state-specific behavior to the current state object.
    - Manages state transitions based on the outcomes of function calls (contracts defined by `CircuitBreakerSwitch`)

### 2. State Management

- `CircuitBreakerState`: Defines the behavior that every state will do at a phrase of the circuit breaker lifecycle
  (e.g., `enter` is called when entering a state, `invocationSucceeds` is invoked when a wrapped function call
  succeeds).
- `ClosedCircuitBreakerState`: Represents a "Healthy" state, which allows requests to pass through.
- `OpenCircuitBreakerState`: Represents a "Tripped" state, which blocks requests from passing through.
- `HalfOpenCircuitBreakerState`:Represents a "Recovering" state, some limited testing requests are allowed to pass
  through, before deciding whether to transition back to Closed or revert to Open.
- `CircuitBreakerSwitch`: Defines the contract for state transitions (e.g., `openCircuit`, `closeCircuit`,
  `attempToCloseCircuit`).

### 3. Logic Components

- `CircuitBreakerInvoker`/`DefaultCircuitBreakerInvoker`: Handle low-level execution of the user's code.
  They are responsible for invoking the unsafe function calls, catch exceptions, and notify the circuit breaker of
  success or failure.
- `CircuitBreakerListener`: An observer interface used to receive notifications when states changed, it then notifies
  these changes to the registered listeners, e.g., logging or metrics systems.
