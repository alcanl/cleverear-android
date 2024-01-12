package com.alcanl.cleverear.sdk.events;

import android.util.Log;
import androidx.annotation.NonNull;
public class ScanEvent extends BLEScannerEvent {
    private static final String TAG = ScanEvent.class.getSimpleName();

    public ScanEvent(String data) {
        super(data);
        Log.i(TAG, data);
    }
    @NonNull
    @Override
    public String toString() {
        return super.toString() + "ScanEvent{" + data + "}";
    }
}
