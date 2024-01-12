package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;

public class BatteryStateChangedEvent extends DeviceSpecificEvent {

    public final int level;
    public BatteryStateChangedEvent(String address, int level) {
        super(address);
        this.level = level;
    }
    @NonNull
    @Override
    public String toString() {
        return super.toString() + ", BatteryStateChangedEvent{" +
                "level=" + level +
                '}';
    }
}
