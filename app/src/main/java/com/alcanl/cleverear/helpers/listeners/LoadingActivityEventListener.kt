package com.alcanl.cleverear.helpers.listeners

import android.util.Log
import android.widget.Toast
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.parseJsonConnectionData
import com.alcanl.cleverear.helpers.parseJsonSenderMacData
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import org.json.JSONException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class LoadingActivityEventListener(activity: LoadingActivity, private val mEventHandler: EventHandler,
                                   private val mConfiguration : ConfigurationModule)
{
    private val mWeakReference = WeakReference(activity)
    private lateinit var mListener : Thread
    fun startEventListener() {

        mListener = thread(isDaemon = true, start = true) {
            while (!mListener.isInterrupted) {
                try {
                    val event = mEventHandler.event
                    val eventSender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)
                    when (event.type) {
                        EventType.kConnectionEvent -> handleConnectionEvent(event, eventSender)
                        else -> {
                            Log.e("event", event.data)
                        }
                    }
                }catch (ex: JSONException)
                {
                    Log.e("parseError", ex.message!!)
                }
                catch (ex: NullPointerException)
                {
                    Log.e("null", ex.message!!)
                }
                catch(ex : InterruptedException)
                {
                    Log.e("thread", ex.message!!)
                }
            }
        }
    }
    private fun handleConnectionEvent(event: Event, eventSender: String)
    {
        val actLoad = mWeakReference.get()!!
        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actLoad)
            if (connectionState == 3 || connectionState == 2) {
                if (!mConfiguration.isDoubleDeviceConnection) {
                    actLoad.runOnUiThread {
                        Toast.makeText(
                            actLoad,
                            R.string.toast_connection_established_text,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    mConfiguration.hearingAid.isConnected = true
                    mConfiguration.isConnected = true
                }
                else {
                    if (eventSender == mConfiguration.doubleDeviceMap[EarSide.Left]?.address) {
                        mConfiguration.doubleDeviceMap[EarSide.Left]?.isConnected = true
                    }
                    else
                        mConfiguration.doubleDeviceMap[EarSide.Right]?.isConnected = true

                    if (mConfiguration.doubleDeviceMap.all { it.value.isConnected }){
                        actLoad.runOnUiThread {
                            Toast.makeText(
                                actLoad,
                                R.string.toast_double_device_connection_established_text,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        mConfiguration.isConnected = true
                    }
                }
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    fun stopListener()
    {
        mListener.interrupt()
    }
}