package com.github.blemale.fuse;

import com.lmax.disruptor.EventHandler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;


class StateMachine implements EventHandler<Event> {
    private interface State extends EventHandler<Event> {
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
        public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
            condition.update(event.status());
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
        public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
            if (event.status() != CallStatus.OPEN) {
                transitionTo(event.status() == CallStatus.SUCCESS ? close : open);
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
        public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
            if (openAt.plusNanos(cooldown).isBefore(clock.instant())) {
                transitionTo(halfOpen);
            }
        }
    }

    private final Open open;
    private final HalfOpen halfOpen;
    private final Close close;
    private volatile State state;

    StateMachine(Condition condition, Duration cooldown) {
        this(condition, cooldown, Clock.systemDefaultZone());
    }

    StateMachine(Condition condition, Duration cooldown, Clock clock) {
        open = new Open(cooldown, clock);
        halfOpen = new HalfOpen();
        close = new Close(condition);

        transitionTo(close);
    }

    public boolean isExecutionAllowed() {
        return state.isExecutionAllowed();
    }

    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
        state.onEvent(event, sequence, endOfBatch);
    }

    private void transitionTo(State newState) {
        this.state = newState;
        newState.enter();
    }
}
