package com.alcanl.cleverear.helpers.service.di.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HA_LEFT_ADDRESS
import com.alcanl.cleverear.helpers.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.helpers.SHARED_PREF_DEVICE
import java.lang.reflect.Method
class DeviceDisconnectBroadcastReceiver(val configuration: ConfigurationModule): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?)
    {
        val action = intent?.action
        val adapter = context?.getSystemService(BluetoothManager::class.java)?.adapter
        val prefs = context?.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)!!
        val leftDevice = prefs.getString(HA_LEFT_ADDRESS, "")!!
        val rightDevice = prefs.getString(HA_RIGHT_ADDRESS, "")!!

        if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED && adapter?.isEnabled!!)
            onReceive(leftDevice, rightDevice, adapter)

    }
    private fun isConnected(device: BluetoothDevice): Boolean
    {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (ex: Exception) {
            throw IllegalStateException(ex)
        }
    }
    @SuppressLint("MissingPermission")
    private fun onReceive(leftDevice: String, rightDevice: String, adapter: BluetoothAdapter)
    {
        try {
            if (leftDevice.isNotBlank() && !isConnected(adapter.getRemoteDevice(leftDevice))) {
                Log.d("debug", String.format("Device %s disconnected", leftDevice))
                if (configuration.isDoubleDeviceConnection) {
                    configuration.doubleDeviceMap[EarSide.Left]?.isConnected = false
                    if (!configuration.doubleDeviceMap[EarSide.Right]?.isConnected!!)
                        configuration.isConnected = false
                }
                else {
                    configuration.hearingAid.isConnected = false
                    configuration.isConnected = false
                }
            }


            if (rightDevice.isNotBlank() && !isConnected(adapter.getRemoteDevice(rightDevice))) {
                Log.d("debug", String.format("Device %s disconnected", rightDevice))

                if (configuration.isDoubleDeviceConnection) {
                    configuration.doubleDeviceMap[EarSide.Right]?.isConnected = false
                    if (!configuration.doubleDeviceMap[EarSide.Left]?.isConnected!!)
                        configuration.isConnected = false
                }
                else {
                    configuration.hearingAid.isConnected = false
                    configuration.isConnected = false
                }
            }


        } catch (_: IllegalStateException) {}
    }
}