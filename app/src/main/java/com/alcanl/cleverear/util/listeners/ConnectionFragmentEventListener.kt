package com.alcanl.cleverear.util.listeners

import android.content.Context
import android.util.Log
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.parseJsonConnectionData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class ConnectionFragmentEventListener @Inject constructor (@ApplicationContext context: Context) {

    @Inject
    lateinit var mEventHandler: EventHandler

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
                    val event = mEventHandler.event
                    if (event.type == EventType.kConnectionEvent) {
                        val connectionEvent = ConnectionEvent(this)
                        connectionEvent.eventSender = parseJsonSenderMacData(event.data, mContext)
                        connectionEvent.connectionData = parseJsonConnectionData(event.data, mContext)
                        EventBus.getDefault().post(connectionEvent)
                    }
                    else
                        Log.d(ConnectionFragmentEventListener::class.simpleName, event.data)
                }

            } catch (ex: NullPointerException) {
                Log.e("null", ex.message!!)
            } catch (ex: InterruptedException) {
                Log.e("thread", ex.message!!)
            } catch (ex: JSONException) {
                Log.e("json", ex.message!!)
            }
        }
    }
    @Synchronized
    fun stopListener()
    {
        mListener?.interrupt()
        mEventHandler.exit()
    }

    fun isListenerOnBoard() : Boolean
    {
        if (mListener == null)
            return false

        return mListener?.isAlive!!
    }
}