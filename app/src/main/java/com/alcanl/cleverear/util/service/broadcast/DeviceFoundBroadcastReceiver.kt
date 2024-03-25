package com.alcanl.cleverear.util.service.broadcast

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import javax.inject.Inject

class DeviceFoundBroadcastReceiver @Inject constructor(): HiltBroadcastReceiver() {
    @Inject
    lateinit var configuration: ConfigurationModule
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        val action = intent?.action

        if (action == BluetoothDevice.ACTION_FOUND) {
            val device  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

            if (device != null && isHearingAid(device)) {
                with(configuration) {
                    if (!isDoubleDeviceConnection) {
                        hearingAid.isDeviceInRange = true
                        Log.d("deviceFound", hearingAid.name)
                    }

                    else
                        if (device.address == doubleDeviceMap[EarSide.Left]?.address) {
                            doubleDeviceMap[EarSide.Left]?.isDeviceInRange = true
                            Log.d("deviceFound", doubleDeviceMap[EarSide.Left]?.name!!)
                        }
                        else {
                            doubleDeviceMap[EarSide.Right]?.isDeviceInRange = true
                            Log.d("deviceFound", doubleDeviceMap[EarSide.Right]?.name!!)
                        }

                    canBondDeviceReConnect = true

                }
            }
        }
    }
    private fun isHearingAid(device: BluetoothDevice) : Boolean
    {
        return if (!configuration.isDoubleDeviceConnection) device.address == configuration.hearingAid.address
            else configuration.doubleDeviceMap.any { it.value.address == device.address }
    }
}