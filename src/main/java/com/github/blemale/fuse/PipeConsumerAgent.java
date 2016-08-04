package com.github.blemale.fuse;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.Pipe;

import static org.agrona.LangUtil.rethrowUnchecked;

class PipeConsumerAgent implements Agent {
    private final Pipe<Event> pipe;
    private final StateMachine stateMachine;

    public PipeConsumerAgent(Pipe<Event> pipe, StateMachine stateMachine) {
        this.pipe = pipe;
        this.stateMachine = stateMachine;
    }

    @Override
    public int doWork() throws Exception {
        return pipe.drain(event -> {
            try {
                stateMachine.onEvent(event, 0, false);
            } catch (Exception ex) {
                rethrowUnchecked(ex);
            }
        });
    }

    @Override
    public String roleName() {
        return "pipe-consumer";
    }

}
