package com.alcanl.cleverear.util.listeners

import android.util.Log
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import org.json.JSONException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

class LoadingActivityEventListener(activity: LoadingActivity, private val mEventHandler: EventHandler,
                                   private val mConfiguration : ConfigurationModule, private val mThreadPool : ExecutorService)
{
    private val mWeakReference = WeakReference(activity)
    private var mListener : Thread? = null
    fun startEventListener() {

        mThreadPool.execute {
            try {
                mListener = Thread.currentThread()
                 while (!mListener?.isInterrupted!!){
                    try {
                        val event = mEventHandler.event
                        val eventSender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)
                        when (event.type) {
                            EventType.kConnectionEvent -> handleConnectionEvent(event, eventSender)
                            else -> {
                                Log.e("eventLoading", event.data)
                            }
                        }
                    } catch (ex: JSONException) {
                        Log.e("parseError", ex.message!!)
                    }
                }
                Log.d("loading", "LoadingListenerStopped")

            } catch (ex: NullPointerException) {
                Log.e("nullTest", ex.message!!)
            }
            catch(ex : InterruptedException) {
                Log.e("thread", ex.message!!)
            }
        }
    }
    private fun handleConnectionEvent(event: Event, eventSender: String)
    {
        val actLoad = mWeakReference.get()!!
        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actLoad)
            if (connectionState == 3) {
                if (!mConfiguration.isDoubleDeviceConnection) {
                    mConfiguration.hearingAid.isConnected = true
                    mConfiguration.isConnected = true
                }
                else {
                    if (eventSender == mConfiguration.doubleDeviceMap[EarSide.Left]?.address) {
                        mConfiguration.doubleDeviceMap[EarSide.Left]?.isConnected = true
                    }
                    else
                        mConfiguration.doubleDeviceMap[EarSide.Right]?.isConnected = true

                    if (mConfiguration.doubleDeviceMap.all { it.value.isConnected })
                        mConfiguration.isConnected = true

                }
            }
        } catch (ex: JSONException)
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