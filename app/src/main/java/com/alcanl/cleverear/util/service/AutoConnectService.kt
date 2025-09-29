package com.alcanl.cleverear.util.service

import android.annotation.SuppressLint
import android.app.Notification
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
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import com.alcanl.cleverear.BuildConfig.ACCOUNT_SID
import com.alcanl.cleverear.BuildConfig.AUTH_TOKEN
import com.alcanl.cleverear.BuildConfig.PHONE_NUMBER
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.util.EMERGENCY_NUMBER
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.util.WHAT_BACKGROUND_CONNECTED
import com.alcanl.cleverear.util.WHAT_BACKGROUND_DEVICE_FALL
import com.alcanl.cleverear.util.WHAT_BACKGROUND_HIGH_TEMPERATURE
import com.alcanl.cleverear.util.WHAT_BACKGROUND_LOW_TEMPERATURE
import com.alcanl.cleverear.util.getLeftSideHearingAid
import com.alcanl.cleverear.util.getRightSideHearingAid
import com.alcanl.cleverear.util.isConnected
import com.alcanl.cleverear.util.isServiceRestarted
import com.alcanl.cleverear.util.notification.createServiceNotification
import com.alcanl.cleverear.util.notification.handleConnectedNotification
import com.alcanl.cleverear.util.notification.handleDeviceFellNotification
import com.alcanl.cleverear.util.notification.handleHighTemperatureNotification
import com.alcanl.cleverear.util.notification.handleLowTemperatureNotification
import com.alcanl.cleverear.util.notification.showNotification
import com.alcanl.cleverear.util.readConfigurations
import com.alcanl.cleverear.util.service.broadcast.BluetoothStateBroadcastReceiver
import com.alcanl.cleverear.util.service.broadcast.DeviceDisconnectBroadcastReceiver
import com.alcanl.cleverear.util.service.broadcast.DeviceFoundBroadcastReceiver
import com.ark.EventHandler
import com.ark.ProductManager
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import com.twilio.Twilio
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private var mConnectorThread: Future<*>? = null
    private var isScanInProgress = false
    private lateinit var mHandler: AutoConnectHandler
    private lateinit var mConnectedNotification: Notification
    private lateinit var mFallenDeviceNotification: Notification
    private lateinit var mHighTemperatureNotification: Notification
    private lateinit var mLowTemperatureNotification: Notification

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        Log.i(AutoConnectService::class.simpleName, "Service On Board")
        initialize(intent)
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        try {
            unregisterReceiver(mDisconnectReceiver)
            unregisterReceiver(mBluetoothStateBroadcastReceiver)
            unregisterReceiver(mDeviceFoundReceiver)
            Twilio.destroy()
            scheduledExecutor.shutdownNow()
            configuration.setServiceStopAndKeepConnectionIfRequired(this)
            Log.i(AutoConnectService::class.simpleName, "Service Stopped")
        } catch (ex: Throwable) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
    }

    private fun handleServiceRestart()
    {
        readConfigurations(configuration, this)
        configuration.apply {

            if (isDoubleDeviceConnection) {
                hearingAidMap[EarSide.Left] = getLeftSideHearingAid(this@AutoConnectService)
                hearingAidMap[EarSide.Right] = getRightSideHearingAid(this@AutoConnectService)
            }
            else if (earSide == EarSide.Left)
                hearingAidMap[EarSide.Left] = getLeftSideHearingAid(this@AutoConnectService)
            else
                hearingAidMap[EarSide.Right] = getRightSideHearingAid(this@AutoConnectService)

            if (hearingAidMap.any { it.value.isEddystoneTLM })
                hearingAidMap.filter { it.value.isEddystoneTLM }.forEach { eddyStoneMap[it.value.side] = EddystoneHearingAid() }
        }
    }

    private fun initialize(intent: Intent?)
    {
        try {
            registerReceiver(mDisconnectReceiver, IntentFilter(ACTION_ACL_DISCONNECTED))
            registerReceiver(mBluetoothStateBroadcastReceiver, IntentFilter(ACTION_STATE_CHANGED))
            registerReceiver(mDeviceFoundReceiver, IntentFilter(IntentFilter(ACTION_FOUND)))
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN)

            if (isServiceRestarted(this) && !configuration.isMainActivityOnScreen)
                handleServiceRestart()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(
                    1,
                    createServiceNotification(this),
                    FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or FOREGROUND_SERVICE_TYPE_LOCATION
                )
            else
                startForeground(1, createServiceNotification(this))

            mHandler = AutoConnectHandler(this)
            configuration.isServiceOnBoard = true
            checkDeviceConnectionState(intent)
            checkDeviceEddystoneValues(intent)

        } catch (ex: Exception) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
    }

    private class AutoConnectHandler(service: AutoConnectService) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()!!

            when (msg.what) {
                WHAT_BACKGROUND_CONNECTED -> showNotification(service.mConnectedNotification, service)
                WHAT_BACKGROUND_DEVICE_FALL -> showNotification(service.mFallenDeviceNotification, service)
                WHAT_BACKGROUND_LOW_TEMPERATURE -> showNotification(service.mLowTemperatureNotification, service)
                WHAT_BACKGROUND_HIGH_TEMPERATURE -> showNotification(service.mHighTemperatureNotification, service)
            }
            super.handleMessage(msg)
        }
    }

    private fun checkDeviceConnectionState(intent: Intent?)
    {
        scheduledExecutor.scheduleWithFixedDelay(({ scheduledThreadCallback(intent!!) }),0, 30, TimeUnit.SECONDS)
    }

    private fun checkDeviceEddystoneValues(intent: Intent?)
    {
        if (configuration.isConnected)
            configuration.hearingAidMap
                .filter { it.value.isEddystoneTLM }
                .map { configuration.eddyStoneMap[it.key]!! }
                .forEach { eddystoneHearingAid -> scheduledExecutor.scheduleWithFixedDelay({ scheduledEddystoneThreadCallback(eddystoneHearingAid, intent!!) },
                    0, 30, TimeUnit.SECONDS) }
    }

    @SuppressLint("MissingPermission")
    private fun scheduledEddystoneThreadCallback(hearingAid: EddystoneHearingAid, intent: Intent)
    {
        if (configuration.isMainActivityOnScreen || !hearingAid.isTrackingActive)
            return

        val bleScanner = getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        val scanCallback = object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult?)
            {
                if (result?.device?.address == hearingAid.macAddress)
                {
                    hearingAid.reSetEddyStoneProperties(EddystoneTLM(17, 0x16, result.scanRecord?.bytes), this@AutoConnectService)
                    var stopScanFlag = false

                    if (hearingAid.isFallen) {
                        mFallenDeviceNotification = handleDeviceFellNotification(intent, this@AutoConnectService)
                        threadPool.execute { sendDeviceFellSMS(hearingAid.deviceName) }
                        mHandler.sendEmptyMessage(WHAT_BACKGROUND_DEVICE_FALL)
                        stopScanFlag = true
                    }

                    else if (hearingAid.isLowTemperature()) {
                        mLowTemperatureNotification = handleLowTemperatureNotification(intent, this@AutoConnectService)
                        mHandler.sendEmptyMessage(WHAT_BACKGROUND_LOW_TEMPERATURE)
                        stopScanFlag = true
                    }

                    else if (hearingAid.isHighTemperature()) {
                        mHighTemperatureNotification = handleHighTemperatureNotification(intent, this@AutoConnectService)
                        mHandler.sendEmptyMessage(WHAT_BACKGROUND_HIGH_TEMPERATURE)
                        stopScanFlag = true
                    }

                    if (stopScanFlag)
                    {
                        isScanInProgress = false
                        bleScanner.stopScan(this)
                    }
                }
            }
        }

        bleScanner.startScan(scanCallback)
        isScanInProgress = true
        mHandler.postDelayed({ if (isScanInProgress) bleScanner.stopScan(scanCallback) }, 10000)
    }
    private fun sendDeviceFellSMS(serialNumber: String)
    {
        try {
            val smsText = getString(R.string.fallen_device_sms_body).format(serialNumber)
            val phoneNumber = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE).getString(EMERGENCY_NUMBER, "")

            val client = OkHttpClient()
            val credential = Credentials.basic(ACCOUNT_SID, AUTH_TOKEN)

            val formBody = FormBody.Builder()
                .add("To", phoneNumber!!)
                .add("From", PHONE_NUMBER)
                .add("Body", smsText)
                .build()

            val request = Request.Builder()
                .url("https://api.twilio.com/2010-04-01/Accounts/$ACCOUNT_SID/Messages.json")
                .header("Authorization", credential)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful)
                Log.d("Twilio", "SMS sent")
            else {
                Log.e("Twilio", "Error: ${response.code()}")
                Log.e("Twilio", "Error: ${response.body()}")
            }

        } catch (ex: Throwable) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
    }
    private fun scheduledThreadCallback(intent: Intent?)
    {
        try {
            configuration.apply {
                if (!isDoubleDeviceConnection) {
                    if (isConnected) {
                        Log.d(AutoConnectService::class.simpleName, "Single Device Connected")
                        return
                    }
                    else if (isBluetoothEnabled) {
                        doConnection(hearingAidMap[earSide]!!)
                        mConnectorThread?.get()
                    }
                }
                else {
                    if (hearingAidMap.all { it.value.isConnected }) {
                        Log.d(AutoConnectService::class.simpleName, "Double Device Connected")
                        return
                    }

                    if (!hearingAidMap[EarSide.Left]?.isConnected!! && isBluetoothEnabled) {
                        doConnection(hearingAidMap[EarSide.Left]!!)
                        mConnectorThread?.get()
                    }

                    if (!hearingAidMap[EarSide.Right]?.isConnected!! && isBluetoothEnabled) {
                        doConnection(hearingAidMap[EarSide.Right]!!)
                        mConnectorThread?.get()
                    }
                }
            }

            if (isConnected()) {
                mConnectedNotification = handleConnectedNotification(intent!!, this)
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
            Log.d(AutoConnectService::class.simpleName, "Bluetooth Off")
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
        return with (configuration) {
            if (hearingAidMap.isEmpty())
                false
            else
                hearingAidMap.all { it.value.isConnected }
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    private fun scheduledThreadConnectDeviceCallback(adapter: BluetoothAdapter, hearingAid: HearingAid)
    {
        try {
            adapter.startDiscovery()
            Thread.sleep(10000)
            configuration.apply {
                if (canBondDeviceReConnect)
                    hearingAid.apply {
                        getPropertiesFromSDK(productManager, eventHandler)
                        communicationAdaptor?.connect()
                        Thread.sleep(10000)
                        isConnected = isConnected(adapter.getRemoteDevice(hearingAid.address))

                        if (isConnected && isDeviceInRange) {
                            isDeviceInRange = false
                            readDeviceConfiguration()
                            if (isEddystoneTLM)
                                eddyStoneMap[side] = EddystoneHearingAid()
                        }
                    }
                else
                    Log.d(AutoConnectService::class.simpleName, "There is not any hearing aid in range!")
            }
        } catch (ex: Throwable) {
            Log.e(AutoConnectService::class.simpleName, ex.message!!)
        }
        finally {
            adapter.cancelDiscovery()
        }
    }
}