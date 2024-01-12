package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;

public abstract class BLEScannerEvent {

    public final String data;
    protected BLEScannerEvent(String data) {
        this.data = data;
    }

    @NonNull
    @Override
    public String toString() {
        return "BluetoothAdapterEvent{" +
                "data='" + data + '\'' +
                '}';
    }
}
