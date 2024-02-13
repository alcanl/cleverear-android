package com.alcanl.cleverear.helpers.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Toast
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.HA_LEFT_ADDRESS
import com.alcanl.cleverear.helpers.HA_LEFT_NAME
import com.alcanl.cleverear.helpers.HA_LEFT_RSSI
import com.alcanl.cleverear.helpers.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.helpers.HA_RIGHT_NAME
import com.alcanl.cleverear.helpers.HA_RIGHT_RSSI
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.helpers.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.helpers.SHARED_PREF_DEVICE
import com.alcanl.cleverear.helpers.WHAT_BACKGROUND_CONNECTED
import com.alcanl.cleverear.helpers.WHAT_BACKGROUND_DISCONNECTED
import com.ark.EventHandler
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class AutoConnectService : Service()
{
    private lateinit var mHandler: Handler
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var configuration: ConfigurationModule
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        initialize()
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }
    @SuppressLint("MissingPermission")
    private fun checkDeviceConnection()
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter

        val leftDevice = getSharedPrefsForLeftDevice()
        val rightDevice = getSharedPrefsForRightDevice()

        if (!configuration.isConnected) {
            for (device in adapter.bondedDevices!!) {

                if (device.address == leftDevice.address) {
                    leftDevice.getPropertiesFromSDK(productManager, eventHandler)
                    leftDevice.communicationAdaptor?.connect()
                    Thread.sleep(5000)
                    mHandler.sendEmptyMessage(WHAT_BACKGROUND_CONNECTED)
                }

                if (device.address == rightDevice.address) {
                    rightDevice.getPropertiesFromSDK(productManager, eventHandler)
                    rightDevice.communicationAdaptor?.connect()
                    Thread.sleep(5000)
                    mHandler.sendEmptyMessage(WHAT_BACKGROUND_CONNECTED)
                }
            }
        }
        stopSelf()
    }
    private fun initialize()
    {
        mHandler = AutoConnectHandler(this)
        threadPool.execute { checkDeviceConnection() }
    }
    private fun getSharedPrefsForLeftDevice() : HearingAid
    {
        val prefs = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
        val deviceAddress = prefs.getString(HA_LEFT_ADDRESS, "")
        val deviceName = prefs.getString(HA_LEFT_NAME, "")
        val deviceRSSI = prefs.getInt(HA_LEFT_RSSI, 0)
        val deviceMANU = prefs.getString(MANUFACTURER_CODE_LEFT, "")

        return HearingAid(deviceAddress!!, deviceName!!, deviceRSSI, deviceMANU!!, EarSide.Left)
    }
    private fun getSharedPrefsForRightDevice() : HearingAid
    {
        val prefs = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
        val deviceAddress = prefs.getString(HA_RIGHT_ADDRESS, "")
        val deviceName = prefs.getString(HA_RIGHT_NAME, "")
        val deviceRSSI = prefs.getInt(HA_RIGHT_RSSI, 0)
        val deviceMANU = prefs.getString(MANUFACTURER_CODE_RIGHT,"")

        return HearingAid(deviceAddress!!, deviceName!!, deviceRSSI, deviceMANU!!, EarSide.Right)
    }
    private class AutoConnectHandler(service: AutoConnectService) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()!!

            when(msg.what) {
                WHAT_BACKGROUND_CONNECTED -> Toast.makeText(service, "Connected to device", Toast.LENGTH_LONG).show()
                WHAT_BACKGROUND_DISCONNECTED -> Toast.makeText(service, "Disconnected from device", Toast.LENGTH_LONG).show()
            }

            super.handleMessage(msg)
        }
    }
}