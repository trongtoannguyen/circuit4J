# Hướng Dẫn Testing Concurrent Async Methods

## Tổng Quan

File `ConcurrentAsyncExecutionTest.java` chứa các test cases để kiểm thử thread-safety và concurrent behavior của Circuit Breaker khi nhiều threads truy cập đồng thời.

## Các Kỹ Thuật Testing Concurrent

### 1. CountDownLatch - Đồng Bộ Hóa Threads

```java
CountDownLatch startLatch = new CountDownLatch(1);
CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
```

**Mục đích:**
- `startLatch`: Đảm bảo tất cả threads start cùng lúc (tạo race condition thực sự)
- `completionLatch`: Chờ tất cả threads hoàn thành trước khi verify kết quả

**Cách hoạt động:**
```java
// Trong mỗi thread
startLatch.await();        // Chờ signal
// ... thực hiện test ...
completionLatch.countDown(); // Báo hiệu hoàn thành

// Trong main test thread
startLatch.countDown();      // Phát signal cho tất cả threads
completionLatch.await();     // Chờ tất cả threads xong
```

### 2. AtomicInteger - Thread-Safe Counters

```java
AtomicInteger successCount = new AtomicInteger(0);
AtomicInteger failureCount = new AtomicInteger(0);
```

**Tại sao dùng AtomicInteger?**
- `int` thông thường KHÔNG thread-safe
- `AtomicInteger` đảm bảo increment operations là atomic
- Tránh race condition khi nhiều threads cùng update counter

**Ví dụ vấn đề với int thông thường:**
```java
// KHÔNG THREAD-SAFE
int count = 0;
// Thread 1: đọc count=0, tính 0+1=1
// Thread 2: đọc count=0, tính 0+1=1  (cùng lúc)
// Thread 1: ghi count=1
// Thread 2: ghi count=1
// Kết quả: count=1 (sai! phải là 2)

// THREAD-SAFE
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet(); // Atomic operation, không bị race condition
```

### 3. ExecutorService - Quản Lý Thread Pool

```java
ExecutorService testExecutor = Executors.newFixedThreadPool(20);
```

**Tại sao cần thread pool?**
- Tạo nhiều threads để simulate concurrent access
- Kiểm soát số lượng threads (tránh tạo quá nhiều)
- Dễ dàng shutdown và cleanup

### 4. CompletableFuture - Async Testing

```java
circuitBreaker.executeAsync(asyncTask)
    .thenAccept(result -> successCount.incrementAndGet())
    .exceptionally(ex -> {
        failureCount.incrementAndGet();
        return null;
    })
    .join(); // Chờ async operation hoàn thành
```

**Lưu ý:**
- `.join()` block thread cho đến khi CompletableFuture complete
- Cần thiết để đảm bảo test không kết thúc trước khi async operations xong

## Các Test Scenarios

### Test 1: Multiple Concurrent Successful Calls
**Mục đích:** Verify circuit breaker xử lý đúng nhiều successful requests đồng thời

**Kỹ thuật:**
- 100 threads cùng gọi `executeAsync()` với successful tasks
- Verify tất cả calls đều succeed
- Đảm bảo không có race condition trong state management

### Test 3: Race Condition Tests
**Mục đích:** Verify state transitions thread-safe

**Kỹ thuật:**
- 20 threads cùng trigger failures
- Verify circuit breaker vẫn hoạt động đúng
- Kiểm tra state cuối cùng là OPEN

**Điểm quan trọng:**
```java
// DefaultCircuitBreaker sử dụng AtomicReference
private final AtomicReference<CircuitBreakerState> currentState;

// compareAndSet đảm bảo chỉ 1 thread thành công transition
private boolean tryTransitionState(CircuitBreakerState from, CircuitBreakerState to) {
    if (currentState.compareAndSet(from, to)) {
        to.enter();
        return true;
    }
    return false;
}
```

### Test 4: High Load Stress Test
**Mục đích:** Verify performance và stability dưới high load

**Kỹ thuật:**
- 1000 concurrent async calls
- Verify tất cả calls được process
- Đảm bảo không có deadlock hoặc resource leak

### Test 5: Circuit Recovery
**Mục đích:** Verify circuit recovery sau reset timeout với concurrent access

**Kỹ thuật:**
1. Trip circuit (gây MAX_FAILURES failures)
2. Verify circuit OPEN
3. Wait RESET_TIMEOUT
4. 10 threads cùng try access
5. Verify ít nhất 1 thread succeed (half-open state)

### Test 6: Timeout Handling
**Mục đích:** Verify timeout detection trong concurrent scenario

**Kỹ thuật:**
- 30 threads, 50% timeout
- Verify timeout exceptions được detect đúng
- Track số lượng timeouts vs successes

## Common Pitfalls (Lỗi Thường Gặp)

### 1. Không Chờ Async Operations
```java
// SAI - Test kết thúc trước khi async operations xong
circuitBreaker.executeAsync(task); // Không chờ
assertEquals(1, successCount.get()); // Sai! Async chưa xong

// ĐÚNG - Chờ async operations
circuitBreaker.executeAsync(task).join(); // Chờ xong
assertEquals(1, successCount.get()); // Đúng
```

### 2. Không Đồng Bộ Thread Start
```java
// SAI - Threads start không đồng thời
for (int i = 0; i < 100; i++) {
    executor.submit(() -> test()); // Mỗi thread start khác thời điểm
}

// ĐÚNG - Tất cả threads start cùng lúc
CountDownLatch startLatch = new CountDownLatch(1);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        startLatch.await(); // Chờ signal
        test();
    });
}
startLatch.countDown(); // Start tất cả cùng lúc
```

### 3. Dùng int Thay Vì AtomicInteger
```java
// SAI - Race condition
int count = 0;
threads.forEach(t -> count++); // Không thread-safe

// ĐÚNG - Thread-safe
AtomicInteger count = new AtomicInteger(0);
threads.forEach(t -> count.incrementAndGet());
```

### 4. Không Cleanup ExecutorService
```java
@AfterEach
void tearDown() {
    executor.shutdown();
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
}
```

## Best Practices

1. **Luôn dùng CountDownLatch** để đồng bộ thread start
2. **Dùng AtomicInteger** cho counters trong concurrent tests
3. **Verify với timeout** để tránh test hang forever
4. **Cleanup resources** trong @AfterEach
5. **Log kết quả** để debug (System.out.println)
6. **Test với nhiều số lượng threads** khác nhau
7. **Repeat tests nhiều lần** để catch intermittent failures

## Kết Luận

Testing concurrent code khó hơn sequential code vì:
- **Non-deterministic**: Kết quả có thể khác nhau mỗi lần chạy
- **Timing-dependent**: Race conditions chỉ xảy ra trong điều kiện cụ thể
- **Hard to reproduce**: Bugs có thể không xuất hiện trong test environment

Các kỹ thuật trong file test này giúp:
- Tạo concurrent scenarios có thể reproduce
- Verify thread-safety một cách reliable
- Catch race conditions và deadlocks
