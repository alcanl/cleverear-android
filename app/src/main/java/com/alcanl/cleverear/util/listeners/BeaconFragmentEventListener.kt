package com.alcanl.cleverear.util.listeners

import android.content.Context
import android.util.Log
import com.alcanl.cleverear.sdk.events.BatteryStateChangedEvent
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.sdk.events.CurrentMemoryCharacteristicChangedEvent
import com.alcanl.cleverear.sdk.events.VolumeChangeEvent
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.MemoryEvent
import com.alcanl.cleverear.util.listeners.event.VolumeEvent
import com.alcanl.cleverear.util.parseJsonBatteryStateData
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonMemoryActiveData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.alcanl.cleverear.util.parseJsonVolumeData
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class BeaconFragmentEventListener @Inject constructor (@ApplicationContext context: Context) {

    @Inject
    lateinit var eventHandler: EventHandler

    @Inject
    lateinit var threadPool: ExecutorService

    private val mContext = context

    private var mListener: Thread? = null

    fun startEventListener()
    {
        threadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener?.isInterrupted!!) {
                    val event = eventHandler.event
                    val eventSender = parseJsonSenderMacData(event.data, mContext)

                    when (event.type) {
                        EventType.kConnectionEvent -> handleConnectionEvent(event)
                        EventType.kBatteryEvent -> handleBatteryEvent(event, eventSender)
                        EventType.kMemoryEvent -> handleMemoryEvent(event, eventSender)
                        EventType.kVolumeEvent -> handleVolumeEvent(event, eventSender)
                        else -> Log.i(BeaconFragmentEventListener::class.simpleName, event.data)
                    }
                }

            } catch (ex: NullPointerException) {
                Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
            } catch (ex: InterruptedException) {
                Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
            } catch (ex: JSONException) {
                Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
            }
        }
    }

    private fun handleConnectionEvent(event: Event)
    {
        try {
            Log.i(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionEvent = ConnectionEvent(this)
            connectionEvent.eventSender = parseJsonSenderMacData(event.data, mContext)
            connectionEvent.connectionData = parseJsonConnectionData(event.data, mContext)

            EventBus.getDefault().post(connectionEvent)

        } catch (ex: JSONException) {
            Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
        }
    }
    private fun handleBatteryEvent(event: Event, eventSender: String)
    {
        try {
            Log.i(BatteryStateChangedEvent::class.java.simpleName, event.data)
            val batteryEvent = BatteryEvent(this)
            batteryEvent.eventSender = eventSender
            batteryEvent.batteryData = parseJsonBatteryStateData(event.data, mContext)

            EventBus.getDefault().post(batteryEvent)

        } catch (ex: JSONException) {
            Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
        }
    }
    private fun handleMemoryEvent(event: Event, eventSender: String) {

        try {
            Log.i(CurrentMemoryCharacteristicChangedEvent::class.java.simpleName, event.data)

            val memoryEvent = MemoryEvent(this)
            memoryEvent.eventSender = eventSender
            memoryEvent.memoryData = parseJsonMemoryActiveData(event.data, mContext)

            EventBus.getDefault().post(memoryEvent)

        } catch (ex: JSONException) {
            Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
        }
    }
    private fun handleVolumeEvent(event: Event, eventSender: String)
    {
        try {
            Log.i(VolumeChangeEvent::class.java.simpleName, event.data)
            val volumeEvent = VolumeEvent(this)
            volumeEvent.eventSender = eventSender
            volumeEvent.volumeData = parseJsonVolumeData(event.data, mContext)

            EventBus.getDefault().post(volumeEvent)
        }
        catch (ex: JSONException) {
            Log.e(BeaconFragmentEventListener::class.simpleName, ex.message!!)
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

        return mListener?.isAlive!!
    }
}