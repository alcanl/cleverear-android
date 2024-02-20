package com.alcanl.cleverear.helpers.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.alcanl.cleverear.R
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
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ProductManager
import com.onsemi.androidble.BleUtil
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
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
    @Inject
    lateinit var scheduledExecutor: ScheduledExecutorService
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        initialize()
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }

    override fun onDestroy() {
        super.onDestroy()
        BleUtil.BleManager_DeInit()

        try {
            if (configuration.isConnected)
                if (configuration.isDoubleDeviceConnection)
                    configuration.doubleDeviceMap.forEach { it.value.endConnectionSession() }
                else
                    configuration.hearingAid.endConnectionSession()

            threadPool.shutdown()
        } catch (_: ArkException) {}
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
    }

    private fun initialize()
    {
        startForeground(1, createNotification())
        mHandler = AutoConnectHandler(this)
        checkConnection()

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

    private fun checkConnection()
    {
        scheduledExecutor.scheduleAtFixedRate(this::scheduledThreadCallback, 0, 2, TimeUnit.MINUTES)
    }
    private fun scheduledThreadCallback()
    {
        with(configuration) {
            if (isConnected)
                return
            else {
                if (isDoubleDeviceConnection) {

                    if (!doubleDeviceMap[EarSide.Left]?.isConnected!!)
                        doubleDeviceMap[EarSide.Left]?.communicationAdaptor?.connect()

                    if (!doubleDeviceMap[EarSide.Right]?.isConnected!!)
                        doubleDeviceMap[EarSide.Right]?.communicationAdaptor?.connect()

                }
                else
                    hearingAid.communicationAdaptor?.connect()

                mHandler.sendEmptyMessage(WHAT_BACKGROUND_CONNECTED)
            }
        }
    }
    private fun createNotification() : Notification
    {
        return NotificationCompat.Builder(this, "CleverEar Demo")
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle("CleverEar Bluetooth Connection")
            .setContentText("Connected with Hearing Aid")
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
}