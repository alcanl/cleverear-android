package com.alcanl.cleverear.util.listeners

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import com.alcanl.cleverear.sdk.events.ScanEvent
import com.alcanl.cleverear.util.ASHA_DEVICE
import com.alcanl.cleverear.util.EMPTY_STRING
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.MANUFACTURER_CODE_LEFT
import com.alcanl.cleverear.util.MANUFACTURER_CODE_RIGHT
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.DeviceFoundEvent
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonScanData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class ConnectionFragmentEventListener @Inject constructor (@ApplicationContext context: Context) {

    @Inject
    lateinit var eventHandler: EventHandler

    @Inject
    lateinit var threadPool: ExecutorService

    @Inject
    lateinit var configuration: ConfigurationModule

    private val mContext = context

    private var mListener: Thread? = null

    @Synchronized
    private fun isHearingAid(manufacturerCode: String): Boolean
    {
        var manufacturerCodeSub = ""

        try {
            manufacturerCodeSub = manufacturerCode.substring(0, 6)
        } catch (_: StringIndexOutOfBoundsException) {}

        return if (!configuration.isDoubleDeviceConnection)
            if (configuration.earSide == EarSide.Left) manufacturerCodeSub == MANUFACTURER_CODE_LEFT
            else manufacturerCodeSub == MANUFACTURER_CODE_RIGHT
        else
            manufacturerCodeSub == MANUFACTURER_CODE_RIGHT || manufacturerCodeSub == MANUFACTURER_CODE_LEFT
    }

    fun startEventListener()
    {
        threadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener?.isInterrupted!!) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        checkBackgroundAshaConnection()

                    val event = eventHandler.event
                    when (event.type) {
                        EventType.kConnectionEvent -> {
                            Log.i(ConnectionEvent::class.java.simpleName, event.data)
                            val connectionEvent = ConnectionEvent(this)
                            connectionEvent.eventSender = parseJsonSenderMacData(event.data, mContext)
                            connectionEvent.connectionData = parseJsonConnectionData(event.data, mContext)
                            EventBus.getDefault().post(connectionEvent)
                        }

                        EventType.kScanEvent -> {
                            Log.i(ScanEvent::class.java.simpleName, event.data)
                            val device = parseJsonScanData(event.data, mContext)
                            if (isHearingAid(device.manufacturerData)) {
                                val deviceFoundEvent = DeviceFoundEvent(this)
                                deviceFoundEvent.hearingAid = device
                                EventBus.getDefault().post(deviceFoundEvent)
                            }
                        }
                        else -> Log.i(ConnectionFragmentEventListener::class.simpleName, event.data)
                    }
                }

            } catch (ex: NullPointerException) {
                Log.e(ConnectionFragmentEventListener::class.simpleName, ex.message!!)
            } catch (ex: InterruptedException) {
                Log.e(ConnectionFragmentEventListener::class.simpleName, ex.message!!)
            } catch (ex: JSONException) {
                Log.e(ConnectionFragmentEventListener::class.simpleName, ex.message!!)
            }
        }
    }

    @SuppressLint("MissingPermission", "InlinedApi")
    private fun checkBackgroundAshaConnection()
    {
        mContext.getSystemService(BluetoothManager::class.java).getConnectedDevices(BluetoothProfile.GATT)
            .filter { it.name == ASHA_DEVICE }
            .forEach { ha ->
                val deviceFoundEvent = DeviceFoundEvent(this@ConnectionFragmentEventListener)
                deviceFoundEvent.hearingAid = DiscoveredDeviceInfo(ha.address, ha.name, 0, EMPTY_STRING)
                EventBus.getDefault().post(deviceFoundEvent)
            }
    }

    @Synchronized
    fun stopListener()
    {
        mListener?.interrupt()
        eventHandler.exit()
    }

    fun isListenerOnBoard() : Boolean
    {
        if (mListener == null)
            return false

        return mListener?.isAlive ?: false
    }
}