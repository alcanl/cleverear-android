package com.alcanl.cleverear.sdk.events;

import androidx.annotation.NonNull;
public class MicChangeEvent extends DeviceSpecificEvent {

    public final byte miclevel;
    public MicChangeEvent(String address, byte miclevel) {
        super(address);
        this.miclevel = miclevel;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + ", MicChangeEvent{" +
                "miclevel=" + miclevel +
                '}';
    }
}
