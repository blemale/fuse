package com.github.blemale.fuse;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.Pipe;

import java.util.function.Consumer;

class PipeConsumerAgent<T> implements Agent {
    private final Pipe<T> pipe;
    private final Consumer<T> consumer;

    public PipeConsumerAgent(Pipe<T> pipe, Consumer<T> consumer) {
        this.pipe = pipe;
        this.consumer = consumer;
    }

    @Override
    public int doWork() throws Exception {
        return pipe.drain(consumer);
    }

    @Override
    public String roleName() {
        return "pipe-consumer";
    }

}
