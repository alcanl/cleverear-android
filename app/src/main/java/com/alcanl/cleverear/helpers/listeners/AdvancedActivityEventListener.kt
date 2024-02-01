package com.alcanl.cleverear.helpers.listeners

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.AdvancedActivity
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.R
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
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import kotlin.concurrent.thread

class AdvancedActivityEventListener(activity: AdvancedActivity, private val mEventHandler: EventHandler,
                                    private val mThreadPool : ExecutorService,
                                    private val mConfiguration : ConfigurationModule
) {

    private val mWeakReference = WeakReference(activity)
    private lateinit var mListener: Thread
    fun startEventListener() {
        mListener = thread(isDaemon = true, start = true) {
            while (!mListener.isInterrupted) {
                try {
                    val event = mEventHandler.event
                    val eventSender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)

                    when (event.type) {
                        EventType.kConnectionEvent -> handleConnectionEvent(event)
                        EventType.kBatteryEvent -> handleBatteryEvent(event, eventSender)
                        EventType.kMemoryEvent -> handleMemoryEvent(event, eventSender)
                        EventType.kVolumeEvent -> handleVolumeEvent(event, eventSender)
                        else -> handleGenericEvent(event)
                    }

                } catch (ex: JSONException) {
                    Log.e("parseError", ex.message!!)
                } catch (ex: NullPointerException) {
                    Log.e("null", ex.message!!)
                } catch (ex: InterruptedException) {
                    Log.e("thread", ex.message!!)
                }
            }
        }

    }
    private fun handleGenericEvent(event: Event)
    {
        Log.e("event", event.data)
    }
    private fun handleConnectionEvent(event: Event)
    {
        val actAdv = mWeakReference.get()!!

        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actAdv)

            with(actAdv) {
                runOnUiThread {
                    if (connectionState == 1) {
                        Toast.makeText(
                            this,
                            R.string.toast_connection_lost_text,
                            Toast.LENGTH_LONG
                        ).show()

                        startActivity(Intent(actAdv, LoadingActivity::class.java))
                        finish()
                        mConfiguration.isConnected = false
                    }
                }
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleBatteryEvent(event: Event, eventSender: String)
    {
        val actAdv = mWeakReference.get()!!
        try {
            Log.e(BatteryStateChangedEvent::class.java.simpleName, event.data)
            val batteryState = parseJsonBatteryStateData(event.data, actAdv)

            mThreadPool.execute {
                with(mConfiguration) {
                    if (!isDoubleDeviceConnection)
                        hearingAid.batteryLevel = batteryState
                    else
                        if (eventSender == doubleDeviceMap[EarSide.Left]?.address)
                            doubleDeviceMap[EarSide.Left]?.batteryLevel = batteryState
                        else
                            doubleDeviceMap[EarSide.Right]?.batteryLevel = batteryState
                }
            }

        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleMemoryEvent(event: Event, eventSender: String) {

        try {
            val actAdv = mWeakReference.get()!!
            Log.e(
                CurrentMemoryCharacteristicChangedEvent::class.java.simpleName,
                event.data
            )
            val memoryState = parseJsonMemoryActiveData(event.data, actAdv)

            mThreadPool.execute {
                with(mConfiguration) {
                    if (!isDoubleDeviceConnection)
                        hearingAid.currentMemory = ParameterSpace.value(memoryState)
                    else
                        if (eventSender == doubleDeviceMap[EarSide.Left]?.address!!)
                            doubleDeviceMap[EarSide.Left]?.currentMemory = ParameterSpace.value(memoryState)
                        else
                            doubleDeviceMap[EarSide.Right]?.currentMemory = ParameterSpace.value(memoryState)
                }
            }
            actAdv.runOnUiThread {
                Toast.makeText(actAdv, String.format(getString(actAdv, R.string.text_active_memory)
                        + (memoryState + 1).toString()), Toast.LENGTH_SHORT).show()
            }

        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleVolumeEvent(event: Event, eventSender: String)
    {
        val actAdv = mWeakReference.get()!!

        try {
            Log.e(VolumeChangeEvent::class.java.simpleName, event.data)
            val volumeState = parseJsonVolumeData(event.data, actAdv)

            mThreadPool.execute {
                with(mConfiguration) {
                    if (!isDoubleDeviceConnection)
                        hearingAid.volume = volumeState
                    else
                        if (eventSender == doubleDeviceMap[EarSide.Left]?.address)
                            doubleDeviceMap[EarSide.Left]?.volume = volumeState
                        else
                            doubleDeviceMap[EarSide.Right]?.volume = volumeState
                }
            }

            actAdv.runOnUiThread {
                Toast.makeText(actAdv, String.format(
                    getString(
                        actAdv,
                        R.string.listener_volume_level_text
                    ) + " %d", volumeState), Toast.LENGTH_SHORT).show()
            }

        }
        catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    fun stopListener()
    {
        mListener.interrupt()
    }
}