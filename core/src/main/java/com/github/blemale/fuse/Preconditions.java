package com.github.blemale.fuse;

class Preconditions {
    private Preconditions() {
        throw new IllegalStateException();
    }

    static void requireArgument(boolean condition, String message) {
        if(!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
