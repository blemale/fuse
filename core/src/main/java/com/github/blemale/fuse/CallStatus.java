package com.github.blemale.fuse;

public enum CallStatus {
    SUCCESS, FAILURE, OPEN;

    boolean isSuccess() {
        return this == SUCCESS;
    }
}
