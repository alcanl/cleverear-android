package com.alcanl.cleverear.util.listeners

import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.alcanl.cleverear.ControlActivity
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

class ControlActivityEventListener(activity: ControlActivity, private val mEventHandler: EventHandler,
                                   private val mThreadPool : ExecutorService,
                                   private val mConfiguration : ConfigurationModule) {

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
                    Log.d("control", "ControlListenerStopped")

                } catch (ex: NullPointerException) {
                        Log.e("null", ex.message!!)
                    } catch (ex: InterruptedException) {
                        Log.e("thread", ex.message!!)
                    }
            }
    }
    private fun handleGenericEvent(event: Event)
    {
        Log.e("eventControl", event.data)
    }
    private fun handleConnectionEvent(event: Event)
    {
        val actCont = mWeakReference.get()!!

        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actCont)

            if (connectionState == 1) {
                with(actCont) {
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
                Thread.currentThread().interrupt()
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleBatteryEvent(event: Event, eventSender: String)
    {
        val actCont = mWeakReference.get()!!
        try {
            Log.e(BatteryStateChangedEvent::class.java.simpleName, event.data)
            val batteryState = parseJsonBatteryStateData(event.data, actCont)

            with(actCont) {
                runOnUiThread {
                    with(mConfiguration) {
                        if (!isDoubleDeviceConnection) {
                            if (hearingAid.side == EarSide.Left)
                                mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                            else
                                mBinding.batteryViewRightDevice.batteryLevel = batteryState

                            hearingAid.batteryLevel = batteryState
                        }
                        else
                            if (eventSender == doubleDeviceMap[EarSide.Left]?.address) {
                                mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                                doubleDeviceMap[EarSide.Left]?.batteryLevel = batteryState
                            }
                            else {
                                mBinding.batteryViewRightDevice.batteryLevel = batteryState
                                doubleDeviceMap[EarSide.Right]?.batteryLevel = batteryState
                            }
                    }
                }
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleMemoryEvent(event: Event, eventSender: String) {

        val actCont = mWeakReference.get()!!
        try {
            Log.e(
                CurrentMemoryCharacteristicChangedEvent::class.java.simpleName,
                event.data
            )
            val memoryState = parseJsonMemoryActiveData(event.data, mWeakReference.get()!!)

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

                actCont.setSelectedMemoryButtonView(getMemoryEventButton(memoryState), memoryState)
            }

        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    private fun handleVolumeEvent(event: Event, eventSender: String)
    {
        val actCont = mWeakReference.get()!!

        try {
            Log.e(VolumeChangeEvent::class.java.simpleName, event.data)
            val volumeState = parseJsonVolumeData(event.data, actCont)

            actCont.runOnUiThread {
                with(mConfiguration) {
                    if (!isDoubleDeviceConnection) {
                        hearingAid.volume = volumeState
                        actCont.mBinding.sliderCommon.value = hearingAid.volume.toFloat()
                    }
                    else
                        if (isDualDeviceConfig) {
                            doubleDeviceMap.forEach {  it.value.volume = volumeState  }
                            actCont.mBinding.sliderCommon.value = doubleDeviceMap[EarSide.Left]?.volume!!.toFloat()
                        }
                        else
                            if (eventSender == doubleDeviceMap[EarSide.Left]?.address) {
                                doubleDeviceMap[EarSide.Left]?.volume = volumeState
                                actCont.mBinding.sliderLeft.value = doubleDeviceMap[EarSide.Left]?.volume!!.toFloat()
                            }
                            else {
                                doubleDeviceMap[EarSide.Right]?.volume = volumeState
                                actCont.mBinding.sliderRight.value = doubleDeviceMap[EarSide.Right]?.volume!!.toFloat()
                            }
                }
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
    private fun getMemoryEventButton(memoryState: Int) : Button
    {
        val actCont = mWeakReference.get()!!

        return when (memoryState) {
            0 -> actCont.mBinding.buttonOne
            1 -> actCont.mBinding.buttonTwo
            2 -> actCont.mBinding.buttonThree
            3 -> actCont.mBinding.buttonFour
            4 -> actCont.mBinding.buttonFive
            5 -> actCont.mBinding.buttonSix
            6 -> actCont.mBinding.buttonSeven
            else -> actCont.mBinding.buttonEight
        }
    }
    fun isListenerOnBoard() : Boolean
    {
        if (mListener == null)
            return false

        return mListener?.isAlive!!

    }
}