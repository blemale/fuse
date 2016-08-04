package com.github.blemale.fuse;

import org.agrona.concurrent.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CircuitBreaker implements AutoCloseable {
    public static class CircuitBreakerOpenException extends RuntimeException {}

    private final StateMachine stateMachine;
    private final QueuedPipe<Event> queuedPipe;
    private final AgentRunner agentRunner;

    public CircuitBreaker(Condition condition, Duration cooldown) {
        stateMachine = new StateMachine(condition, cooldown);
        queuedPipe = new ManyToOneConcurrentArrayQueue<>(1024);
        PipeConsumerAgent agent = new PipeConsumerAgent(queuedPipe, stateMachine);
        agentRunner =
                new AgentRunner(
                        new YieldingIdleStrategy(),
                        t -> {},
                        null,
                        agent
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

    public <T> T execute(Supplier<T> action) {
        try {
            if (stateMachine.isExecutionAllowed()) {
                return action.get();
            } else {
                report(CallStatus.OPEN);
                throw new CircuitBreakerOpenException();
            }
        } catch (Throwable ex) {
            report(CallStatus.FAILURE);
            throw ex;
        }
    }

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
        while (!queuedPipe.offer(Event.create(callStatus)));
    }
}
