package com.alcanl.cleverear.util.listeners

import android.content.Context
import android.util.Log
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.alcanl.cleverear.util.parseJsonBatteryStateData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.ark.EventHandler
import com.ark.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class SplashActivityEventListener @Inject constructor (@ApplicationContext context: Context) {

    @Inject
    lateinit var mEventHandler: EventHandler

    @Inject
    lateinit var mThreadPool: ExecutorService

    private val mContext = context

    private lateinit var mListener: Thread

    fun startEventListener()
    {

        mThreadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener.isInterrupted) {
                    val event = mEventHandler.event
                    if (event.type == EventType.kBatteryEvent) {
                        val batteryEvent = BatteryEvent(this)
                        batteryEvent.eventSender = parseJsonSenderMacData(event.data, mContext)
                        batteryEvent.batteryData = parseJsonBatteryStateData(event.data, mContext)
                        EventBus.getDefault().post(batteryEvent)
                    }
                    else
                        Log.d(SplashActivityEventListener::class.simpleName, event.data)
                }
                Log.d(SplashActivityEventListener::class.simpleName, "Splash Listener Stopped")
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
        mListener.interrupt()
        mEventHandler.exit()
    }
}