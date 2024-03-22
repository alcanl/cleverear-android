package com.alcanl.cleverear.util.service.broadcast

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.content.Context
import android.content.Intent
import com.alcanl.cleverear.di.module.ConfigurationModule
import javax.inject.Inject

class BluetoothStateBroadcastReceiver @Inject constructor(): HiltBroadcastReceiver() {
    lateinit var configuration: ConfigurationModule
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        val action = intent?.action

        if (action == ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            if (state == BluetoothAdapter.STATE_OFF) {
                configuration.apply {
                    isBluetoothEnabled = false

                    if (isDoubleDeviceConnection)
                        doubleDeviceMap.forEach { it.value.isConnected = false }
                    else
                        hearingAid.isConnected = false

                    isConnected = false
                }
            }

            if (state == BluetoothAdapter.STATE_ON)
                configuration.isBluetoothEnabled = true
        }
    }
}