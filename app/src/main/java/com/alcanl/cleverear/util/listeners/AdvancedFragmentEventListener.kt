package com.alcanl.cleverear.util.listeners

import android.content.Context
import android.util.Log
import com.alcanl.cleverear.util.parseJsonBatteryStateData
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonMemoryActiveData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.alcanl.cleverear.util.parseJsonVolumeData
import com.alcanl.cleverear.sdk.events.BatteryStateChangedEvent
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.sdk.events.CurrentMemoryCharacteristicChangedEvent
import com.alcanl.cleverear.sdk.events.VolumeChangeEvent
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.MemoryEvent
import com.alcanl.cleverear.util.listeners.event.VolumeEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class AdvancedFragmentEventListener @Inject constructor (@ApplicationContext context: Context) {

    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService

    private var mListener: Thread? = null
    private val mContext = context

    fun startEventListener()
    {
        threadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener?.isInterrupted!!) {
                    try {
                        val event = eventHandler.event
                        val eventSender = parseJsonSenderMacData(event.data, mContext)

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
        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionEvent = ConnectionEvent(this)
            connectionEvent.connectionData = parseJsonConnectionData(event.data, mContext)

            EventBus.getDefault().post(connectionEvent)

        } catch (ex: JSONException) {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleBatteryEvent(event: Event, eventSender: String)
    {
        try {
            Log.e(BatteryStateChangedEvent::class.java.simpleName, event.data)
            val batteryEvent = BatteryEvent(this)
            batteryEvent.eventSender = eventSender
            batteryEvent.batteryData = parseJsonBatteryStateData(event.data, mContext)

            EventBus.getDefault().post(batteryEvent)

        } catch (ex: JSONException) {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleMemoryEvent(event: Event, eventSender: String) {

        try {
            Log.e(CurrentMemoryCharacteristicChangedEvent::class.java.simpleName, event.data)

            val memoryEvent = MemoryEvent(this)
            memoryEvent.eventSender = eventSender
            memoryEvent.memoryData = parseJsonMemoryActiveData(event.data, mContext)

            EventBus.getDefault().post(memoryEvent)

        } catch (ex: JSONException) {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleVolumeEvent(event: Event, eventSender: String)
    {
        try {
            Log.e(VolumeChangeEvent::class.java.simpleName, event.data)
            val volumeEvent = VolumeEvent(this)
            volumeEvent.eventSender = eventSender
            volumeEvent.volumeData = parseJsonVolumeData(event.data, mContext)

            EventBus.getDefault().post(volumeEvent)
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
        eventHandler.exit()
    }
    @Synchronized
    fun isListenerOnBoard() : Boolean
    {
        if (mListener == null)
            return false

        return mListener?.isAlive!!
    }
}