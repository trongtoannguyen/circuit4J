package com.example;

import java.util.concurrent.CompletableFuture;

/**
 * This class throw exception for testing failure service only
 */
public class ExternalService {

    public void get() {
        throw new RuntimeException();
    }

    public CompletableFuture<Void> getAsync() {
        return CompletableFuture.failedFuture(new RuntimeException());
    }
}