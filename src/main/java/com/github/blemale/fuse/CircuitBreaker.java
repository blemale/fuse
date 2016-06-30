package com.github.blemale.fuse;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class CircuitBreaker {
    public static class CircuitBreakerOpenException extends RuntimeException {}

    private final StateMachine stateMachine;
    private final RingBuffer<Event> ringBuffer;

    public CircuitBreaker(Condition condition, Duration cooldown) {
        stateMachine = new StateMachine(condition, cooldown);

        Disruptor<Event> disruptor = new Disruptor<>(Event::new, 1024, Executors.defaultThreadFactory());
        disruptor.handleEventsWith(stateMachine);
        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();
    }

    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> action) {
        try {
            if (stateMachine.isExecutionAllowed()) {
                return action.get().whenCompleteAsync(this::reportResult);
            } else {
                report(CallStatus.OPEN);
                CompletableFuture<T> f = new CompletableFuture<>();
                f.completeExceptionally(new CircuitBreakerOpenException());
                return f;
            }
        } catch (Throwable ex) {
            report(CallStatus.FAILURE);
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(ex);
            return f;
        }
    }

    private <T> void reportResult(T t, Throwable ex) {
        if (ex != null) {
            report(CallStatus.FAILURE);
        } else {
            report(CallStatus.SUCCESS);
        }
    }

    private void report(CallStatus callStatus) {
        ringBuffer.publishEvent((e, seq, s) -> e.setCallStatus(s), callStatus);
    }
}
