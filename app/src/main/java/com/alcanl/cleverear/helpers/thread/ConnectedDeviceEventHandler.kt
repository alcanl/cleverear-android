package com.alcanl.cleverear.helpers.thread

import android.util.Log
import android.widget.Toast
import com.alcanl.cleverear.ControlActivity
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.parseJsonBatteryStateData
import com.alcanl.cleverear.helpers.parseJsonConnectionData
import com.alcanl.cleverear.helpers.parseJsonMemoryActiveData
import com.alcanl.cleverear.helpers.parseJsonSenderMacData
import com.alcanl.cleverear.sdk.events.AuxChangeEvent
import com.alcanl.cleverear.sdk.events.BatteryStateChangedEvent
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.sdk.events.CurrentMemoryCharacteristicChangedEvent
import com.alcanl.cleverear.sdk.events.MicChangeEvent
import com.alcanl.cleverear.sdk.events.VolumeChangeEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import com.ark.ParameterSpace
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import kotlin.math.log

class ConnectedDeviceEventHandler(activity: ControlActivity, private val mEventHandler: EventHandler,
                                  private val mThreadPool : ExecutorService) {

    private val mWeakReference = WeakReference(activity)
    fun startEventListener() {
        mThreadPool.execute {
            while (true) {
                try {
                    val event = mEventHandler.event
                    val eventSender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)

                    if (event.type == EventType.kConnectionEvent)
                        handleConnectionEvent(event)
                    else if (event.type == EventType.kBatteryEvent) {
                        handleBatteryEvent(event, eventSender)
                    } else if (event.type == EventType.kMemoryEvent) {
                        handleMemoryEvent(event, eventSender)
                    } else if (event.type == EventType.kVolumeEvent) {
                        Log.e("event", event.data)
                        Log.e(VolumeChangeEvent::class.java.simpleName, event.data)
                    } else if (event.type == EventType.kExitEvent) {
                        Log.e("event", event.data)
                        Log.e("exit", event.data)
                    } else if (event.type == EventType.kActiveEvent) {
                        Log.e("event", event.data)
                        Log.e("active", event.data)
                    } else if (event.type == EventType.kUnknownEvent) {
                        Log.e("event", event.data)
                        Log.e("unknown", event.data)
                    } else if (event.type == EventType.kECMemoryEvent) {
                        Log.e("event", event.data)
                        Log.e("ecMemory", event.data)
                    } else if (event.type == EventType.kAuxAttenuationEvent) {
                        Log.e("event", event.data)
                        Log.e(AuxChangeEvent::class.java.simpleName, event.data)
                    } else if (event.type == EventType.kMicAttenuationEvent) {
                        Log.e("event", event.data)
                        Log.e(MicChangeEvent::class.java.simpleName, event.data)
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
        try {
            Log.e("event", event.data)
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState =
                parseJsonConnectionData(event.data, mWeakReference.get()!!)
            if (connectionState == 1) {
                mWeakReference.get()?.runOnUiThread {
                    Toast.makeText(
                        mWeakReference.get()!!,
                        "Connection Lost, Please Reconnect To Device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mWeakReference.get()?.mIsConnected = false
            } else if (connectionState == 3) {
                mWeakReference.get()?.runOnUiThread {
                    Toast.makeText(
                        mWeakReference.get()!!,
                        "Connection Established With The Device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mWeakReference.get()?.mIsConnected = true
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

            mWeakReference.get()?.runOnUiThread {
                if (!mWeakReference.get()?.mIsDoubleDeviceConnection!!)
                    if (mWeakReference.get()?.mHearingAid?.side == EarSide.Left)
                        mWeakReference.get()?.mBinding?.batteryViewLeftDevice?.batteryLevel =
                            batteryState
                    else
                        mWeakReference.get()?.mBinding?.batteryViewRightDevice?.batteryLevel =
                            batteryState
                else
                    if (eventSender == (mWeakReference.get()?.mDoubleDeviceMap?.get(EarSide.Left) as HearingAid).address)
                        mWeakReference.get()?.mBinding?.batteryViewLeftDevice?.batteryLevel =
                            batteryState
                    else
                        mWeakReference.get()?.mBinding?.batteryViewRightDevice?.batteryLevel =
                            batteryState
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleMemoryEvent(event: Event, eventSender: String) {
        try {
            Log.e("event", event.data)
            Log.e(
                CurrentMemoryCharacteristicChangedEvent::class.java.simpleName,
                event.data
            )
            val memoryState = parseJsonMemoryActiveData(event.data, mWeakReference.get()!!)

            mThreadPool.execute {
                if (!mWeakReference.get()?.mIsDoubleDeviceConnection!!)
                    mWeakReference.get()?.mHearingAid?.currentMemory =
                        ParameterSpace.value(memoryState)
                else
                    if (eventSender == (mWeakReference.get()?.mDoubleDeviceMap?.get(EarSide.Left) as HearingAid).address)
                        (mWeakReference.get()?.mDoubleDeviceMap!![EarSide.Left] as HearingAid).currentMemory =
                            ParameterSpace.value(memoryState)
                    else
                        (mWeakReference.get()?.mDoubleDeviceMap!![EarSide.Right] as HearingAid).currentMemory =
                            ParameterSpace.value(memoryState)
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
}