package com.alcanl.cleverear.helpers.listeners

import android.util.Log
import android.widget.Toast
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.parseJsonBatteryStateData
import com.alcanl.cleverear.helpers.parseJsonConnectionData
import com.alcanl.cleverear.helpers.parseJsonMemoryActiveData
import com.alcanl.cleverear.helpers.parseJsonSenderMacData
import com.alcanl.cleverear.helpers.parseJsonVolumeData
import com.alcanl.cleverear.sdk.events.BatteryStateChangedEvent
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.sdk.events.CurrentMemoryCharacteristicChangedEvent
import com.alcanl.cleverear.sdk.events.VolumeChangeEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import com.ark.ParameterSpace
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import kotlin.concurrent.thread

class LoadingActivityEventListener(activity: LoadingActivity, private val mEventHandler: EventHandler,
                                   private val mThreadPool : ExecutorService,
                                   private val mConfiguration : ConfigurationModule)
{
    private val mWeakReference = WeakReference(activity)
    @Volatile
    private var isOnBoard = true

    fun startEventListener() {

        thread(isDaemon = true, start = true) {
            while (isOnBoard) {
                try {
                    val event = mEventHandler.event
                    val eventSender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)

                    when (event.type) {
                        EventType.kConnectionEvent -> handleConnectionEvent(event)
                        EventType.kBatteryEvent -> handleBatteryEvent(event, eventSender)
                        EventType.kMemoryEvent -> handleMemoryEvent(event, eventSender)
                        EventType.kVolumeEvent -> handleVolumeEvent(event, eventSender)
                        else -> {
                            Log.e("event", event.data)
                            Log.e("ecMemory", event.data)
                        }
                    }
                }catch (ex: JSONException)
                {
                    Log.e("parseError", ex.message!!)
                }
            }
        }
    }
    private fun handleConnectionEvent(event: Event)
    {
        val actLoad = mWeakReference.get()!!
        try {
            Log.e("event", event.data)
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actLoad)
            if (connectionState == 1) {
                actLoad.runOnUiThread {
                    Toast.makeText(
                        actLoad,
                        "Connection Lost, Please Reconnect To Device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mConfiguration.isConnected = false
            } else if (connectionState == 3) {
                actLoad.runOnUiThread {
                    Toast.makeText(
                        mWeakReference.get()!!,
                        "Connection Established With The Device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mConfiguration.isConnected = true
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleBatteryEvent(event: Event, eventSender: String)
    {
        try {
            Log.e("event", event.data)
            Log.e(BatteryStateChangedEvent::class.java.simpleName, event.data)
            val batteryState = parseJsonBatteryStateData(event.data, mWeakReference.get()!!)

            if (!mConfiguration.isDoubleDeviceConnection)
                 mConfiguration.hearingAid.batteryLevel = batteryState
            else
                if (eventSender == mConfiguration.doubleDeviceMap[EarSide.Left]?.address)
                        mConfiguration.doubleDeviceMap[EarSide.Left]?.batteryLevel = batteryState
                    else
                        mConfiguration.doubleDeviceMap[EarSide.Right]?.batteryLevel = batteryState
        }
         catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleMemoryEvent(event: Event, eventSender: String)
    {
        try {
            Log.e("event", event.data)
            Log.e(CurrentMemoryCharacteristicChangedEvent::class.java.simpleName, event.data)
            val memoryState = parseJsonMemoryActiveData(event.data, mWeakReference.get()!!)

            mThreadPool.execute {
                if (mConfiguration.isDoubleDeviceConnection)
                    mConfiguration.hearingAid.currentMemory = ParameterSpace.value(memoryState)
                else
                    if (eventSender == mConfiguration.doubleDeviceMap[EarSide.Left]?.address)
                        mConfiguration.doubleDeviceMap[EarSide.Left]?.currentMemory = ParameterSpace.value(memoryState)
                    else
                        mConfiguration.doubleDeviceMap[EarSide.Right]?.currentMemory = ParameterSpace.value(memoryState)
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleVolumeEvent(event: Event, eventSender: String)
    {
        try {
            Log.e("event", event.data)
            Log.e(VolumeChangeEvent::class.java.simpleName, event.data)
            val volumeState = parseJsonVolumeData(event.data, mWeakReference.get()!!)

            mThreadPool.execute {
                if (mConfiguration.isDoubleDeviceConnection)
                    mConfiguration.hearingAid.volume = volumeState
                else
                    if (eventSender == mConfiguration.doubleDeviceMap[EarSide.Left]?.address)
                        mConfiguration.doubleDeviceMap[EarSide.Left]?.volume = volumeState
                    else
                        mConfiguration.doubleDeviceMap[EarSide.Right]?.volume = volumeState
            }

        }
        catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    fun stopListener()
    {
        isOnBoard = false
    }
}