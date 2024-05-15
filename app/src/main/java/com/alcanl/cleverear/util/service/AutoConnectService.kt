package com.alcanl.cleverear.util.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.bluetooth.BluetoothDevice.ACTION_FOUND
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
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_BACKGROUND_CONNECTED
import com.alcanl.cleverear.util.isConnected
import com.alcanl.cleverear.util.service.broadcast.DeviceDisconnectBroadcastReceiver
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.util.service.broadcast.BluetoothStateBroadcastReceiver
import com.alcanl.cleverear.util.service.broadcast.DeviceFoundBroadcastReceiver
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class AutoConnectService : Service()
{
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var scheduledExecutor: ScheduledExecutorService
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var mDisconnectReceiver : DeviceDisconnectBroadcastReceiver
    @Inject
    lateinit var mDeviceFoundReceiver : DeviceFoundBroadcastReceiver
    @Inject
    lateinit var mBluetoothStateBroadcastReceiver : BluetoothStateBroadcastReceiver

    private lateinit var mConnectorThread: Future<*>
    private lateinit var mHandler: Handler
    private lateinit var mConnectedNotification: Notification
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        initialize(intent)
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(mDisconnectReceiver)
            unregisterReceiver(mBluetoothStateBroadcastReceiver)
            unregisterReceiver(mDeviceFoundReceiver)

            with(configuration) {
                if (isConnected)
                    if (isDoubleDeviceConnection)
                        doubleDeviceMap.forEach { it.value.endConnectionSession() }
                    else
                        hearingAid.endConnectionSession()

                isServiceOnBoard = false
            }

        } catch (_: ArkException) {}
        catch (_: IllegalArgumentException) {}
    }
    private fun initialize(intent: Intent?)
    {
        registerReceiver(mDisconnectReceiver, IntentFilter(ACTION_ACL_DISCONNECTED))
        registerReceiver(mBluetoothStateBroadcastReceiver, IntentFilter(ACTION_STATE_CHANGED))
        registerReceiver(mDeviceFoundReceiver, IntentFilter(IntentFilter(ACTION_FOUND)))
        startForeground(1, createServiceNotification())
        mHandler = AutoConnectHandler(this)
        configuration.isServiceOnBoard = true
        checkDeviceConnectionState(intent)
    }
    private class AutoConnectHandler(service: AutoConnectService) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()!!

            when (msg.what) {
            WHAT_BACKGROUND_CONNECTED -> service.showNotification(service.mConnectedNotification)
            }
            super.handleMessage(msg)
        }
    }
    private fun checkDeviceConnectionState(intent: Intent?)
    {
        scheduledExecutor.scheduleWithFixedDelay(({ scheduledThreadCallback(intent!!) }),0, 1, TimeUnit.MINUTES)
    }
    private fun scheduledThreadCallback(intent: Intent?)
    {
        try {
            with(configuration) {
                if (!isDoubleDeviceConnection) {
                    if (isConnected) {
                        Log.d(ConnectionStateChangedEvent::class.java.simpleName, "Connected")
                        return
                    } else if (isBluetoothEnabled) {
                        doConnection(hearingAid)
                        mConnectorThread.get()
                    }
                }
                else {
                    if (doubleDeviceMap.all { it.value.isConnected }) {
                        Log.d(ConnectionStateChangedEvent::class.java.simpleName, "DoubleDeviceConnected")
                        return
                    }

                    if (!doubleDeviceMap[EarSide.Left]?.isConnected!! && isBluetoothEnabled) {
                        doConnection(doubleDeviceMap[EarSide.Left]!!)
                        mConnectorThread.get()
                    }

                    if (!doubleDeviceMap[EarSide.Right]?.isConnected!! && isBluetoothEnabled) {
                        doConnection(doubleDeviceMap[EarSide.Right]!!)
                        mConnectorThread.get()
                    }
                }
            }

            if (isConnected()) {
                mConnectedNotification = handleConnectedNotification(intent!!)
                configuration.apply {
                    isConnected = true
                    canBondDeviceReConnect = false
                }
                mHandler.sendEmptyMessage(WHAT_BACKGROUND_CONNECTED)
            }
        } catch (ex: ExecutionException) {
            Log.e("service", ex.message!!)
        } catch (ex : InterruptedException) {
            Log.e("threadEx", ex.message!!)
        }
    }
    private fun doConnection(hearingAid: HearingAid)
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter

        if (!adapter.isEnabled) {
            Log.e("Bluetooth", "Bluetooth Off")
            return
        }

        if (isConnected(adapter.getRemoteDevice(hearingAid.address))) {
            mConnectorThread = threadPool.submit {
                try {
                    hearingAid.apply {
                        isConnected = true
                        getPropertiesFromSDK(productManager, eventHandler)
                        communicationAdaptor?.connect()
                        Thread.sleep(10000)
                        readDeviceConfiguration()
                    }
                } catch (_: InterruptedException) {}
                configuration.setConnections()
            }
            return
        }

        mConnectorThread = threadPool.submit { scheduledThreadConnectDeviceCallback(adapter, hearingAid) }
    }
    private fun isConnected() : Boolean
    {
        return if (configuration.isDoubleDeviceConnection)
                configuration.doubleDeviceMap[EarSide.Left]?.isConnected!! && configuration.doubleDeviceMap[EarSide.Right]?.isConnected!!
            else
                configuration.hearingAid.isConnected
    }
    private fun checkDeviceConnectionState(hearingAid: HearingAid, adapter: BluetoothAdapter)
    {
        hearingAid.apply {
            isConnected = isConnected(adapter.getRemoteDevice(address))
            Log.e("Connection", isConnected.toString())
        }
    }
    @Synchronized
    private fun scheduledThreadConnectDeviceCallback(adapter: BluetoothAdapter, hearingAid: HearingAid)
    {
        try {
            startDeviceScan(adapter)
            Thread.sleep(10000)
            with(configuration) {
                if (canBondDeviceReConnect) {
                    hearingAid.getPropertiesFromSDK(productManager, eventHandler)
                    hearingAid.communicationAdaptor?.connect()
                    Thread.sleep(10000)
                    checkDeviceConnectionState(hearingAid, adapter)

                    if (hearingAid.isConnected && hearingAid.isDeviceInRange)
                        hearingAid.apply {
                            isDeviceInRange = false
                            readDeviceConfiguration()
                        }
                    else {}
                }
                else
                    Log.d("deviceNotFound", "There is not any hearing aid in range!")
            }
        } catch (ex: InterruptedException) {
            Log.e("scheduler", ex.message!!)
        } catch (_: Throwable) {}
        finally {
            stopDeviceScan(adapter)
        }
    }
    private fun createServiceNotification() : Notification
    {
        return NotificationCompat.Builder(this, getString(R.string.service_notification_start_channel_id))
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle(getString(R.string.service_notification_start_service_text))
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    private fun handleConnectedNotification(intent: Intent) : Notification
    {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(getString(R.string.service_notification_channel_id), getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))

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
        return NotificationCompat.Builder(this, getString(R.string.service_notification_channel_id))
            .setSmallIcon(R.drawable.cleverear_logo)
            .setContentTitle(getString(R.string.notification_reconnect_text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    @SuppressLint("MissingPermission")
    private fun startDeviceScan(adapter: BluetoothAdapter)
    {
        adapter.startDiscovery()
    }
    @SuppressLint("MissingPermission")
    private fun stopDeviceScan(adapter: BluetoothAdapter)
    {
        adapter.cancelDiscovery()

    }

}