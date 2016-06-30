package com.github.blemale.fuse;

class Event {
    private CallStatus callStatus;

    public CallStatus status() {
        return callStatus;
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }
}
