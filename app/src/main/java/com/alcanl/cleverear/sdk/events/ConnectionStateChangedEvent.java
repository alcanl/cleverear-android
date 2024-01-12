package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;

public class ConnectionStateChangedEvent extends DeviceSpecificEvent {

    public final int connectionStatus;

    public ConnectionStateChangedEvent(String address, int connectionStatus) {
        super(address);
        this.connectionStatus = connectionStatus;
    }
    @NonNull
    @Override
    public String toString() {
        return super.toString() + ", ConnectionStateChangedEvent{" +
                "connectionStatus=" + connectionStatus +
                '}';
    }
}
