package com.alcanl.cleverear.util.listeners

import android.content.Context
import android.util.Log
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class LoadingActivityEventListener @Inject constructor (@ApplicationContext context: Context)
{
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool : ExecutorService

    private val mContext = context
    private var mListener : Thread? = null

    fun startEventListener() {

        threadPool.execute {
            try {
                mListener = Thread.currentThread()
                 while (!mListener?.isInterrupted!!){
                    try {
                        val event = eventHandler.event
                        val eventSender = parseJsonSenderMacData(event.data, mContext)
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
        try {
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionEvent = ConnectionEvent(this)
            connectionEvent.connectionData = parseJsonConnectionData(event.data, mContext)
            connectionEvent.eventSender = eventSender

            EventBus.getDefault().post(connectionEvent)

        } catch (ex: JSONException) {
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