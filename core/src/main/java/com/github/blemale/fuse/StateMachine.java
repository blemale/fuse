package com.github.blemale.fuse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


class StateMachine implements Consumer<CallStatus> {
    private interface State extends Consumer<CallStatus> {
        void enter();
        boolean isExecutionAllowed();
    }

    private class Close implements State {
        private final Condition condition;

        public Close(Condition condition) {
            this.condition = condition;
        }

        @Override
        public void enter() {
            condition.reset();
        }

        @Override
        public boolean isExecutionAllowed() {
            return true;
        }

        @Override
        public void accept(CallStatus status) {
            condition.update(status);
            if(!condition.isTrue()) {
                transitionTo(open);
            }
        }
    }

    private class HalfOpen implements State {
        private final AtomicBoolean canTry = new AtomicBoolean(true);

        @Override
        public void enter() {
            canTry.set(true);
        }

        @Override
        public boolean isExecutionAllowed() {
            return canTry.compareAndSet(true, false);
        }

        @Override
        public void accept(CallStatus status) {
            if (status != CallStatus.OPEN) {
                transitionTo(status == CallStatus.SUCCESS ? close : open);
            }
        }
    }

    private class Open implements State {
        private final Clock clock;
        private final long cooldown;
        private Instant openAt = Instant.now();

        public Open(Duration cooldown, Clock clock) {
            this.cooldown = cooldown.toNanos();
            this.clock = clock;
        }

        @Override
        public void enter() {
            openAt = clock.instant();
        }

        @Override
        public boolean isExecutionAllowed() {
            return false;
        }

        @Override
        public void accept(CallStatus status) {
            if (openAt.plusNanos(cooldown).isBefore(clock.instant())) {
                transitionTo(halfOpen);
            }
        }
    }

    private final Open open;
    private final HalfOpen halfOpen;
    private final Close close;
    private volatile State state;

    private final Optional<CountDownLatch> latch;

    StateMachine(Condition condition, Duration cooldown, Clock clock) {
        this(condition, cooldown, clock, Optional.empty());
    }

    StateMachine(Condition condition, Duration cooldown, Clock clock, Optional<CountDownLatch> latch) {
        open = new Open(cooldown, clock);
        halfOpen = new HalfOpen();
        close = new Close(condition);

        this.latch = latch;

        transitionTo(close);
    }

    public boolean isExecutionAllowed() {
        return state.isExecutionAllowed();
    }

    @Override
    public void accept(CallStatus status)  {
        state.accept(status);
        latch.ifPresent(CountDownLatch::countDown);
    }

    private void transitionTo(State newState) {
        this.state = newState;
        newState.enter();
    }
}
