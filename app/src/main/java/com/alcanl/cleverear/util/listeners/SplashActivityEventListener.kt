package com.alcanl.cleverear.util.listeners

import android.util.Log
import com.alcanl.cleverear.SplashActivity
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.parseJsonBatteryStateData
import com.alcanl.cleverear.util.parseJsonSenderMacData
import com.ark.EventHandler
import com.ark.EventType
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

class SplashActivityEventListener(private val mEventHandler: EventHandler,
                                  private val mThreadPool: ExecutorService,
                                  private val mConfiguration: ConfigurationModule,
                                  activity: SplashActivity) {
    private val mWeakReference = WeakReference(activity)
    private lateinit var mListener: Thread
    fun startEventListener()
    {

        mThreadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener.isInterrupted) {
                    val event = mEventHandler.event
                    Log.e("eventSplash", event.data)
                    if (event.type == EventType.kBatteryEvent) {
                        val battery = parseJsonBatteryStateData(event.data, mWeakReference.get()!!)
                        val sender = parseJsonSenderMacData(event.data, mWeakReference.get()!!)
                        mConfiguration.apply {
                            if (isDoubleDeviceConnection)
                                if (sender == doubleDeviceMap[EarSide.Left]?.address)
                                    doubleDeviceMap[EarSide.Left]?.batteryLevel = battery
                                else
                                    doubleDeviceMap[EarSide.Right]?.batteryLevel = battery
                            else
                                hearingAid.batteryLevel = battery
                        }
                    }
                }
                Log.d("splash", "SplashListenerStopped")
            } catch (ex: NullPointerException) {
                Log.e("null", ex.message!!)
            } catch (ex: InterruptedException) {
                Log.e("thread", ex.message!!)
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