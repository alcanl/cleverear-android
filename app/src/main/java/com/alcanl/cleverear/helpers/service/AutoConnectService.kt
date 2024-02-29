package com.alcanl.cleverear.helpers.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.WHAT_BACKGROUND_CONNECTED
import com.alcanl.cleverear.helpers.isConnected
import com.alcanl.cleverear.helpers.service.receiver.DeviceDisconnectBroadcastReceiver
import com.ark.ArkException
import com.onsemi.androidble.BleUtil
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutionException
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
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var scheduledExecutor: ScheduledExecutorService
    private lateinit var mConnectedNotification: Notification
    private val mDisconnectReceiver = DeviceDisconnectBroadcastReceiver()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        mConnectedNotification = handleConnectedNotification(intent!!)
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
            with(configuration) {
                if (isConnected)
                    if (isDoubleDeviceConnection)
                        doubleDeviceMap.forEach { it.value.endConnectionSession() }
                    else
                        hearingAid.endConnectionSession()
            }

            threadPool.shutdown()
        } catch (_: ArkException) {}
    }
    private fun initialize()
    {
        registerReceiver(mDisconnectReceiver, IntentFilter(ACTION_ACL_DISCONNECTED))
        startForeground(1, createNotification())
        mHandler = AutoConnectHandler(this)
        configuration.isServiceOnBoard = true
        startScheduler()

    }
    private class AutoConnectHandler(service: AutoConnectService) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()!!

            when(msg.what) {
                WHAT_BACKGROUND_CONNECTED -> service.showNotification(service.mConnectedNotification)
            }

            super.handleMessage(msg)
        }
    }

    private fun startScheduler()
    {
        scheduledExecutor.scheduleAtFixedRate(this::scheduledThreadCallback, 0, 1, TimeUnit.MINUTES)
    }
    private fun scheduledThreadCallback()
    {
        try {
            with(configuration) {
                if (isConnected)
                    return
                else {
                    val adapter = getSystemService(BluetoothManager::class.java).adapter
                    if (isDoubleDeviceConnection)
                        scheduledDoubleDeviceCallback(adapter)
                    else
                        scheduledSingleDeviceCallback(adapter)
                }
            }
            if (isConnected()) {
                mHandler.sendEmptyMessage(WHAT_BACKGROUND_CONNECTED)
                configuration.isConnected = true
            }
        } catch (ex: ExecutionException) {
            Log.e("service", ex.message!!)
        }
    }
    private fun isConnected() : Boolean
    {
        return if (configuration.isDoubleDeviceConnection)
                configuration.doubleDeviceMap[EarSide.Left]?.isConnected!! && configuration.doubleDeviceMap[EarSide.Right]?.isConnected!!
            else
                configuration.hearingAid.isConnected
    }
    private fun scheduledDoubleDeviceCallback(adapter: BluetoothAdapter)
    {
        try {
            with(configuration) {
                if (!doubleDeviceMap[EarSide.Left]?.isConnected!!) {
                    doubleDeviceMap[EarSide.Left]?.communicationAdaptor?.connect()

                    Thread.sleep(6000)
                    startScheduler(doubleDeviceMap[EarSide.Left]!!, adapter)

                    if (doubleDeviceMap[EarSide.Left]?.isConnected!!)
                        doubleDeviceMap[EarSide.Left]?.readDeviceConfiguration()

                }

                if (!doubleDeviceMap[EarSide.Right]?.isConnected!!) {
                    doubleDeviceMap[EarSide.Right]?.communicationAdaptor?.connect()

                    Thread.sleep(6000)
                    startScheduler(doubleDeviceMap[EarSide.Right]!!, adapter)

                    if (doubleDeviceMap[EarSide.Right]?.isConnected!!)
                        doubleDeviceMap[EarSide.Right]?.readDeviceConfiguration()

                }
            }
        } catch (ex: InterruptedException) {
            Log.e("scheduler", ex.message!!)
        }
    }
    private fun startScheduler(hearingAid: HearingAid, adapter: BluetoothAdapter)
    {
        hearingAid.apply {
            isConnected = isConnected(adapter.getRemoteDevice(address))
        }
    }
    private fun scheduledSingleDeviceCallback(adapter: BluetoothAdapter)
    {
        try {
            with(configuration) {
                hearingAid.communicationAdaptor?.connect()
                Thread.sleep(6000)
                startScheduler(hearingAid, adapter)

                if (hearingAid.isConnected)
                    hearingAid.readDeviceConfiguration()

            }
        } catch (ex: InterruptedException) {
            Log.e("scheduler", ex.message!!)
        }

    }
    private fun createNotification() : Notification
    {
        return NotificationCompat.Builder(this, "CleverEar Demo")
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle("CleverEar Hearing Aid")
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    private fun handleConnectedNotification(intent: Intent) : Notification
    {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("Connected", "CleverEar", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val pendingIntent = PendingIntent.getActivity(this, 5, intent, PendingIntent.FLAG_IMMUTABLE)

        return createConnectedNotification(pendingIntent)
    }
    private fun showNotification(notification: Notification)
    {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(2, notification)
    }
    private fun createConnectedNotification(pendingIntent: PendingIntent) : Notification
    {
        return NotificationCompat.Builder(this, "Connected")
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle("Hearing Aid Re-Connected")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

}