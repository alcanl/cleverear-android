package com.alcanl.cleverear.util.listeners

import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.AdvancedActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.parseJsonBatteryStateData
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonMemoryActiveData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.alcanl.cleverear.util.parseJsonVolumeData
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

class AdvancedActivityEventListener(activity: AdvancedActivity, private val mEventHandler: EventHandler,
                                    private val mThreadPool : ExecutorService,
                                    private val mConfiguration : ConfigurationModule
) {

    private val mWeakReference = WeakReference(activity)
    private var mListener: Thread? = null
    fun startEventListener() {
        mThreadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener?.isInterrupted!!) {
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
                    }
                }

                Log.d("advanced", "AdvancedListenerStopped")
            } catch (ex: NullPointerException) {
                Log.e("null", ex.message!!)
            } catch (ex: InterruptedException) {
                Log.e("thread", ex.message!!)
            }
        }
    }
    private fun handleGenericEvent(event: Event)
    {
        Log.e("eventAdvanced", event.data)
    }
    private fun handleConnectionEvent(event: Event)
    {
        val actAdv = mWeakReference.get()!!

        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actAdv)

            if (connectionState == 1) {
                with(actAdv) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            R.string.toast_connection_lost_text,
                            Toast.LENGTH_SHORT
                        ).show()
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
                        hearingAidMap[earSide]?.batteryLevel = batteryState
                    else
                        if (eventSender == hearingAidMap[EarSide.Left]?.address)
                            hearingAidMap[EarSide.Left]?.batteryLevel = batteryState
                        else
                            hearingAidMap[EarSide.Right]?.batteryLevel = batteryState
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
                        hearingAidMap[earSide]?.currentMemory = ParameterSpace.value(memoryState)
                    else
                        if (eventSender == hearingAidMap[EarSide.Left]?.address!!)
                            hearingAidMap[EarSide.Left]?.currentMemory = ParameterSpace.value(memoryState)
                        else
                            hearingAidMap[EarSide.Right]?.currentMemory = ParameterSpace.value(memoryState)
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
                        hearingAidMap[earSide]?.volume = volumeState
                    else
                        if (eventSender == hearingAidMap[EarSide.Left]?.address)
                            hearingAidMap[EarSide.Left]?.volume = volumeState
                        else
                            hearingAidMap[EarSide.Right]?.volume = volumeState
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
    @Synchronized
    fun stopListener()
    {
        mListener?.interrupt()
        mEventHandler.exit()
    }
    @Synchronized
    fun isListenerOnBoard() : Boolean
    {
        if (mListener == null)
            return false

        return mListener?.isAlive!!

    }
}