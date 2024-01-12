package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;

public abstract class DeviceSpecificEvent {

    public final String address;

    protected DeviceSpecificEvent(String address) {
        this.address = address;
    }
    @NonNull
    @Override
    public String toString() {
        return "DeviceSpecificEvent{" +
                "address='" + address + '\'' +
                '}';
    }
}
