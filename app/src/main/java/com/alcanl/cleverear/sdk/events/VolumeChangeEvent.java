package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;

public class VolumeChangeEvent extends DeviceSpecificEvent {

    public final byte volume;
    public VolumeChangeEvent(String address, byte volume) {
        super(address);
        this.volume = volume;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + ", VolumeChangeEvent{" +
                "volume=" + volume +
                '}';
    }
}
