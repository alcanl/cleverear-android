package com.alcanl.cleverear.helpers.service

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.WHAT_BACKGROUND_CONNECTED
import com.alcanl.cleverear.helpers.WHAT_BACKGROUND_NOT_CONNECTED
import com.alcanl.cleverear.helpers.service.di.receiver.DeviceDisconnectBroadcastReceiver
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ProductManager
import com.onsemi.androidble.BleUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    private val mDisconnectReceiver = DeviceDisconnectBroadcastReceiver(configuration)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        registerReceiver(mDisconnectReceiver, IntentFilter(ACTION_ACL_DISCONNECTED))
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
        unregisterReceiver(mDisconnectReceiver)

        try {
            if (configuration.isConnected)
                if (configuration.isDoubleDeviceConnection)
                    configuration.doubleDeviceMap.forEach { it.value.endConnectionSession() }
                else
                    configuration.hearingAid.endConnectionSession()

            threadPool.shutdown()
        } catch (_: ArkException) {}
    }
    private fun initialize()
    {
        startForeground(1, createNotification())
        mHandler = AutoConnectHandler(this)
        checkConnection()

    }
    private class AutoConnectHandler(service: AutoConnectService) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()!!

            when(msg.what) {
                WHAT_BACKGROUND_CONNECTED -> Toast.makeText(service, "Connected to device", Toast.LENGTH_LONG).show()
                WHAT_BACKGROUND_NOT_CONNECTED -> Toast.makeText(service, "Could not connected to device", Toast.LENGTH_LONG).show()
            }

            super.handleMessage(msg)
        }
    }

    private fun checkConnection()
    {
        scheduledExecutor.scheduleAtFixedRate(this::scheduledThreadCallback, 0, 1, TimeUnit.MINUTES)
    }
    private fun scheduledThreadCallback()
    {
        with(configuration) {
            if (isConnected)
                return
            else
                if (isDoubleDeviceConnection)
                    scheduledDoubleDeviceCallback()
                else
                    scheduledSingleDeviceCallback()
        }

        mHandler.sendEmptyMessage(if (isConnected()) WHAT_BACKGROUND_CONNECTED else WHAT_BACKGROUND_NOT_CONNECTED)
    }
    private fun isConnected() : Boolean
    {
        return if (configuration.isDoubleDeviceConnection)
                configuration.doubleDeviceMap[EarSide.Left]?.isConnected!! && configuration.doubleDeviceMap[EarSide.Right]?.isConnected!!
            else
                configuration.hearingAid.isConnected
    }
    private fun scheduledDoubleDeviceCallback()
    {
        if (!configuration.doubleDeviceMap[EarSide.Left]?.isConnected!!) {
            configuration.doubleDeviceMap[EarSide.Left]?.communicationAdaptor?.connect()
            runBlocking {
                delay(8000)
                configuration.doubleDeviceMap[EarSide.Left]?.readDeviceConfiguration()
            }
        }

        if (!configuration.doubleDeviceMap[EarSide.Right]?.isConnected!!) {
            configuration.doubleDeviceMap[EarSide.Right]?.communicationAdaptor?.connect()
            runBlocking {
                delay(8000)
                configuration.doubleDeviceMap[EarSide.Right]?.readDeviceConfiguration()
            }
        }
    }
    private fun scheduledSingleDeviceCallback()
    {
        configuration.hearingAid.communicationAdaptor?.connect()
        runBlocking {
            delay(6000)
            configuration.hearingAid.readDeviceConfiguration()
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