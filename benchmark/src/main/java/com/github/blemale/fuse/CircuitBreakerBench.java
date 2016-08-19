package com.github.blemale.fuse;

import akka.actor.ActorSystem;
import net.jodah.failsafe.Failsafe;
import org.openjdk.jmh.annotations.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.time.temporal.ChronoUnit.SECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsAppend = "-Djmh.stack.lines=3")
@Threads(1)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class CircuitBreakerBench {

    @Param(value = {"baseline", "fuse", "akka", "failsafe"})
    String cbType;
    CB circuitBreaker;
    String result = "foo";

    @Setup
    public void setUp() {
        circuitBreaker = CB.create(cbType);
    }

    @TearDown
    public void tearDown() throws Exception {
        circuitBreaker.close();
    }

    @Benchmark
    public String success() {
        return circuitBreaker.execute(() -> result);
    }
}

interface CB extends AutoCloseable {
    <T> T execute(Supplier<T> action);

    static CB create(String type) {
        switch (type) {
            case "baseline":
                return new Baseline();
            case "fuse":
                return new FuseCircuitBreaker();
            case "akka":
                return new AkkaCircuitBreaker();
            case "failsafe":
                return new FailsafeCircuitBreaker();
            default:
                throw new IllegalArgumentException();
        }
    }
}

class Baseline implements CB {

    @Override
    public <T> T execute(Supplier<T> action) {
        return action.get();
    }

    @Override
    public void close() throws Exception {
    }
}

class FuseCircuitBreaker implements CB {
    private final com.github.blemale.fuse.CircuitBreaker circuitBreaker;

    FuseCircuitBreaker() {
        circuitBreaker = new CircuitBreaker(new Condition.FailureCount(10), Duration.of(1, SECONDS));
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return circuitBreaker.execute(action);
    }

    @Override
    public void close() throws Exception {
        circuitBreaker.close();
    }
}

class AkkaCircuitBreaker implements CB {
    private final ActorSystem system;
    private final akka.pattern.CircuitBreaker circuitBreaker;

    AkkaCircuitBreaker() {
        system = ActorSystem.create("CircuitBreakerBench");
        circuitBreaker = new akka.pattern.CircuitBreaker(
                system.dispatcher(),
                system.scheduler(),
                10,
                scala.concurrent.duration.Duration.create(1, "s"),
                scala.concurrent.duration.Duration.create(1, "s")
        );
    }


    @Override
    public <T> T execute(Supplier<T> action) {
        return circuitBreaker.callWithSyncCircuitBreaker(action::get);
    }

    @Override
    public void close() throws Exception {
        system.shutdown();
    }
}

class FailsafeCircuitBreaker implements CB {
    private final net.jodah.failsafe.CircuitBreaker circuitBreaker;

    public FailsafeCircuitBreaker() {
        circuitBreaker =
                new net.jodah.failsafe.CircuitBreaker().withFailureThreshold(10).withDelay(1, TimeUnit.SECONDS);
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return Failsafe.with(circuitBreaker).get(action::get);
    }

    @Override
    public void close() throws Exception {}
}
