package com.alcanl.cleverear.helpers.listeners

import android.util.Log
import android.widget.Toast
import com.alcanl.cleverear.LoadingActivity
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.parseJsonConnectionData
import com.alcanl.cleverear.sdk.events.ConnectionStateChangedEvent
import com.ark.Event
import com.ark.EventHandler
import com.ark.EventType
import org.json.JSONException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class LoadingActivityEventListener(activity: LoadingActivity, private val mEventHandler: EventHandler,
                                   private val mThreadPool : ExecutorService,
                                   private val mConfiguration : ConfigurationModule)
{
    private val mWeakReference = WeakReference(activity)

    private lateinit var isOnBoard : Future<*>

    fun startEventListener() {

        isOnBoard =  mThreadPool.submit {
            while (!Thread.interrupted()) {
                try {
                    val event = mEventHandler.event
                    when (event.type) {
                        EventType.kConnectionEvent -> handleConnectionEvent(event)
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
    private fun handleConnectionEvent(event: Event)
    {
        val actLoad = mWeakReference.get()!!
        try {
            Log.e("event", event.data)
            Log.e(ConnectionStateChangedEvent::class.java.simpleName, event.data)
            val connectionState = parseJsonConnectionData(event.data, actLoad)
            if (connectionState == 1) {
                actLoad.runOnUiThread {
                    Toast.makeText(
                        actLoad,
                        "Connection Lost, Please Reconnect To Device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mConfiguration.isConnected = false
            } else if (connectionState == 3) {
                actLoad.runOnUiThread {
                    Toast.makeText(
                        mWeakReference.get()!!,
                        "Connection Established With The Device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mConfiguration.isConnected = true
            }
        } catch (ex: JSONException)
        {
            Log.e("parseError", ex.message!!)
        }
    }
    fun stopListener()
    {
        isOnBoard.cancel(true)
    }
}