package com.alcanl.cleverear.helpers.listeners

import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import com.alcanl.cleverear.ControlActivity
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.parseJsonBatteryStateData
import com.alcanl.cleverear.helpers.parseJsonConnectionData
import com.alcanl.cleverear.helpers.parseJsonMemoryActiveData
import com.alcanl.cleverear.helpers.parseJsonSenderMacData
import com.alcanl.cleverear.helpers.parseJsonVolumeData
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
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class ControlActivityEventListener(activity: ControlActivity, private val mEventHandler: EventHandler,
                                   private val mThreadPool : ExecutorService,
                                   private val mConfiguration : ConfigurationModule) {

    private val mWeakReference = WeakReference(activity)
    private lateinit var isOnBoard : Future<*>
    fun startEventListener() {
        isOnBoard = mThreadPool.submit {
            while (!Thread.interrupted()) {
                try {
                    val event = mEventHandler.event
                    val eventSender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)

                    when (event.type) {
                        EventType.kConnectionEvent -> handleConnectionEvent(event)
                        EventType.kBatteryEvent -> handleBatteryEvent(event, eventSender)
                        EventType.kMemoryEvent -> handleMemoryEvent(event, eventSender)
                        EventType.kVolumeEvent -> handleVolumeEvent(event, eventSender)
                        EventType.kAuxAttenuationEvent -> handleGenericEvent(event, AuxChangeEvent::class.java)
                        EventType.kMicAttenuationEvent -> handleGenericEvent(event, MicChangeEvent::class.java)
                        else -> Log.e("event", event.data)
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
    private fun handleGenericEvent(event: Event, clazz: Class<*>)
    {
        Log.e("event", event.data)
        Log.e("sender", clazz::class.java.simpleName)
    }
    private fun handleConnectionEvent(event: Event)
    {
        val actCont = mWeakReference.get()!!

        try {
            Log.e("event", event.data)
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actCont)

            with(actCont) {
                runOnUiThread {
                    if (connectionState == 1) {
                        Toast.makeText(
                            this,
                            R.string.toast_connection_lost_text,
                            Toast.LENGTH_LONG
                        ).show()

                        startActivity(Intent(actCont, LoadingActivity::class.java))
                        finish()
                        mConfiguration.isConnected = false
                    }
                    else if (connectionState == 3) {
                        Toast.makeText(
                            this,
                            R.string.toast_connection_established_text,
                            Toast.LENGTH_LONG
                        ).show()
                        mConfiguration.isConnected = true
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
        val actCont = mWeakReference.get()!!
        try {
            Log.e("event", event.data)
            Log.e(BatteryStateChangedEvent::class.java.simpleName, event.data)
            val batteryState = parseJsonBatteryStateData(event.data, actCont)

            with(actCont) {
                runOnUiThread {
                    with(mConfiguration) {
                        if (!isDoubleDeviceConnection)
                            if (hearingAid.side == EarSide.Left)
                                mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                            else
                                mBinding.batteryViewRightDevice.batteryLevel = batteryState
                        else
                            if (eventSender == doubleDeviceMap[EarSide.Left]?.address)
                                mBinding.batteryViewLeftDevice.batteryLevel = batteryState
                            else
                                mBinding.batteryViewRightDevice.batteryLevel = batteryState
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
            Log.e("event", event.data)
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
            Log.e("event", event.data)
            Log.e(VolumeChangeEvent::class.java.simpleName, event.data)
            val volumeState = parseJsonVolumeData(event.data, actCont)

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

            actCont.runOnUiThread {
                Toast.makeText(actCont, String.format(getString(actCont,
                    R.string.listener_volume_level_text) + " %d", volumeState), Toast.LENGTH_SHORT).show()
            }

        }
        catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    fun stopListener()
    {
        isOnBoard.cancel(true)
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
}