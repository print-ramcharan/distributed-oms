package com.oms.eventcontracts.events;

import java.io.Serializable;

public abstract class BaseEvent implements Serializable {
    private int eventVersion = 1;

    public BaseEvent() {
    }

    public BaseEvent(int eventVersion) {
        this.eventVersion = eventVersion;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(int eventVersion) {
        this.eventVersion = eventVersion;
    }
}
