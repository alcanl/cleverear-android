package com.alcanl.cleverear.util.service.broadcast

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import javax.inject.Inject

class AshaDeviceConnectionCaptureBroadcastReceiver  @Inject constructor() : HiltBroadcastReceiver() {
    @Inject
    lateinit var configuration : ConfigurationModule
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        val action = intent?.action
        val device : BluetoothDevice

        if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )!!
            else
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

            with(configuration) {
                if (!isDoubleDeviceConnection && hearingAid.address == device.address) {
                    hearingAid.isConnected = true
                    isConnected = true
                }
                else {
                    if (device.address == doubleDeviceMap[EarSide.Right]?.address)
                        doubleDeviceMap[EarSide.Right]?.isConnected = true

                    if (device.address == doubleDeviceMap[EarSide.Left]?.address)
                        doubleDeviceMap[EarSide.Left]?.isConnected = true

                    if (doubleDeviceMap.all { it.value.isConnected })
                        isConnected = true
                }
            }
        }
    }
}