package com.github.blemale.fuse;

class Event {
    private CallStatus callStatus;

    public CallStatus status() {
        return callStatus;
    }

    public static Event create(CallStatus callStatus) {
        Event event = new Event();
        event.setCallStatus(callStatus);
        return event;
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }
}
