package com.github.blemale.fuse;

import org.agrona.concurrent.*;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class CircuitBreaker implements AutoCloseable {
    public static class CircuitBreakerOpenException extends RuntimeException {
        private static CircuitBreakerOpenException INSTANCE = new CircuitBreakerOpenException();
    }

    private final StateMachine stateMachine;
    private final QueuedPipe<CallStatus> queuedPipe;
    private final AgentRunner agentRunner;
    private final IdleStrategy idleStrategy;

    public CircuitBreaker(Condition condition, Duration cooldown) {
        this(condition, cooldown, Clock.systemDefaultZone(), Optional.empty());
    }

    CircuitBreaker(Condition condition, Duration cooldown, Clock clock, Optional<CountDownLatch> latch) {
        Objects.requireNonNull(condition);
        Objects.requireNonNull(cooldown);
        Objects.requireNonNull(clock);
        Objects.requireNonNull(latch);

        idleStrategy = new YieldingIdleStrategy();
        stateMachine = new StateMachine(condition, cooldown, clock, latch);
        queuedPipe = new ManyToOneConcurrentArrayQueue<>(128);
        agentRunner =
                new AgentRunner(
                        new YieldingIdleStrategy(),
                        t -> {},
                        null,
                        new PipeConsumerAgent<>(queuedPipe, stateMachine)
                );
        AgentRunner.startOnThread(agentRunner);
    }

    public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> action) {
        try {
            if (stateMachine.isExecutionAllowed()) {
                return action.get().whenCompleteAsync(this::reportResult);
            } else {
                report(CallStatus.OPEN);
                CompletableFuture<T> f = new CompletableFuture<>();
                f.completeExceptionally(CircuitBreakerOpenException.INSTANCE);
                return f;
            }
        } catch (Throwable ex) {
            report(CallStatus.FAILURE);
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(ex);
            return f;
        }
    }

    public <T> T execute(Supplier<T> action) {
        try {
            if (stateMachine.isExecutionAllowed()) {
                T result = action.get();
                report(CallStatus.SUCCESS);
                return result;
            } else {
                report(CallStatus.OPEN);
                throw CircuitBreakerOpenException.INSTANCE;
            }
        } catch (Throwable ex) {
            report(CallStatus.FAILURE);
            throw ex;
        }
    }

    @Override
    public void close() {
        agentRunner.close();
    }

    private <T> void reportResult(T t, Throwable ex) {
        if (ex != null) {
            report(CallStatus.FAILURE);
        } else {
            report(CallStatus.SUCCESS);
        }
    }

    private void report(CallStatus callStatus) {
        while (!queuedPipe.offer(callStatus)){
            idleStrategy.idle();
        };
    }
}
