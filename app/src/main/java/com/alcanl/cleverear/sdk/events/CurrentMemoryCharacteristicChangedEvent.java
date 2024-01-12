package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;

public class CurrentMemoryCharacteristicChangedEvent extends DeviceSpecificEvent {
    public final byte currentMemory;

    public CurrentMemoryCharacteristicChangedEvent(String address, byte currentMemory) {
        super(address);
        this.currentMemory = currentMemory;
    }
    @NonNull
    @Override
    public String toString() {
        return super.toString() + ", CurrentMemoryValueChanged{" +
                "Memory=" + currentMemory +
                '}';
    }
}
