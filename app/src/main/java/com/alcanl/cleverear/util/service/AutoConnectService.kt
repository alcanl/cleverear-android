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
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.util.EMERGENCY_NUMBER
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.util.WHAT_BACKGROUND_CONNECTED
import com.alcanl.cleverear.util.isConnected
import com.alcanl.cleverear.util.service.broadcast.BluetoothStateBroadcastReceiver
import com.alcanl.cleverear.util.service.broadcast.DeviceDisconnectBroadcastReceiver
import com.alcanl.cleverear.util.service.broadcast.DeviceFoundBroadcastReceiver
import com.ark.EventHandler
import com.ark.ProductManager
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    private lateinit var mHandler: AutoConnectHandler
    private lateinit var mConnectedNotification: Notification


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        Log.i(AutoConnectService::class.simpleName, "onStartCommand")
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
                        hearingAidMap.forEach { it.value.endConnectionSession() }
                    else
                        hearingAidMap[earSide]?.endConnectionSession()

                isServiceOnBoard = false
            }
            Log.i(AutoConnectService::class.simpleName, "onDestroyCalled")
        } catch (ex: Throwable) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
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
        checkDeviceEddystoneValues()
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
        scheduledExecutor.scheduleWithFixedDelay(({ scheduledThreadCallback(intent!!) }),0, 90, TimeUnit.SECONDS)
    }

    private fun checkDeviceEddystoneValues()
    {
        if (configuration.isConnected && !configuration.isMainActivityOnScreen)
            configuration.hearingAidMap
                .filter { it.value.isEddystoneTLM }
                .filter { configuration.eddyStoneMap[it.key]?.isTrackingActive!! }
                .map { configuration.eddyStoneMap[it.key]!! }
                .forEach { eddystoneHearingAid -> scheduledExecutor.scheduleWithFixedDelay({ scheduledEddystoneThreadCallback(eddystoneHearingAid) },
                    0, 1, TimeUnit.MINUTES) }
    }

    @SuppressLint("MissingPermission")
    private fun scheduledEddystoneThreadCallback(hearingAid: EddystoneHearingAid)
    {
        val bleScanner = getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        val scanCallback = object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult?)
            {
                if (result?.device?.address == hearingAid.macAddress)
                    hearingAid.reSetEddyStoneProperties(EddystoneTLM(17, 0x16, result.scanRecord?.bytes))
            }
        }

        bleScanner.startScan(scanCallback)

        runBlocking {
            delay(5000)
        }

        bleScanner.stopScan(scanCallback)

        if (hearingAid.isTrackingActive && hearingAid.isFallen)
            sendDeviceFellSMS()

    }

    private fun sendDeviceFellSMS()
    {
        try {
            val smsText = getString(R.string.service_notification_channel_id)
            val phoneNumber = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE).getString(EMERGENCY_NUMBER, "")
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, smsText, null, null)

        } catch (ex: Throwable) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
    }

    private fun scheduledThreadCallback(intent: Intent?)
    {
        try {
            if (configuration.stopServiceFlag) {
                configuration.stopServiceFlag = false
                scheduledExecutor.shutdownNow()
                stopSelf()
                return
            }

            with(configuration) {
                if (!isDoubleDeviceConnection) {
                    if (isConnected) {
                        Log.d(ConnectionStateChangedEvent::class.java.simpleName, "Single Device Connected")
                        return
                    } else if (isBluetoothEnabled) {
                        doConnection(hearingAidMap[earSide]!!)
                        mConnectorThread.get()
                    }
                }
                else {
                    if (hearingAidMap.all { it.value.isConnected }) {
                        Log.d(ConnectionStateChangedEvent::class.java.simpleName, "Double Device Connected")
                        return
                    }

                    if (!hearingAidMap[EarSide.Left]?.isConnected!! && isBluetoothEnabled) {
                        doConnection(hearingAidMap[EarSide.Left]!!)
                        mConnectorThread.get()
                    }

                    if (!hearingAidMap[EarSide.Right]?.isConnected!! && isBluetoothEnabled) {
                        doConnection(hearingAidMap[EarSide.Right]!!)
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
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        } catch (ex : InterruptedException) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
    }

    private fun doConnection(hearingAid: HearingAid)
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter

        if (!adapter.isEnabled) {
            Log.e(AutoConnectService::class.simpleName, "Bluetooth off")
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
                        if (isEddystoneTLM)
                            configuration.eddyStoneMap[this.side] = EddystoneHearingAid()
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
                configuration.hearingAidMap[EarSide.Left]?.isConnected!! && configuration.hearingAidMap[EarSide.Right]?.isConnected!!
            else
                configuration.hearingAidMap[configuration.earSide]?.isConnected ?: false
    }
    private fun checkDeviceConnectionState(hearingAid: HearingAid, adapter: BluetoothAdapter)
    {
        hearingAid.isConnected = isConnected(adapter.getRemoteDevice(hearingAid.address))
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
                            if (isEddystoneTLM)
                                eddyStoneMap[this.side] = EddystoneHearingAid()
                        }
                    else {}
                }
                else
                    Log.i(AutoConnectService::class.simpleName, "There is not any hearing aid in range!")
            }
        } catch (ex: Throwable) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
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