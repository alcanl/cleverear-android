package com.alcanl.cleverear.util.service.broadcast

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.content.Context
import android.content.Intent
import com.alcanl.cleverear.di.module.ConfigurationModule
import javax.inject.Inject

class BluetoothStateBroadcastReceiver @Inject constructor(): HiltBroadcastReceiver() {
    @Inject
    lateinit var configuration: ConfigurationModule
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        val action = intent?.action

        if (action == ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            if (state == BluetoothAdapter.STATE_OFF) {
                configuration.apply {
                    try {
                        isBluetoothEnabled = false

                        if (isDoubleDeviceConnection)
                            hearingAidMap.forEach { it.value.isConnected = false }
                        else
                            hearingAidMap[earSide]?.isConnected = false

                    } catch (_: UninitializedPropertyAccessException) {}
                    finally {
                        isConnected = false
                    }
                }
            }

            if (state == BluetoothAdapter.STATE_ON)
                configuration.isBluetoothEnabled = true
        }
    }
}