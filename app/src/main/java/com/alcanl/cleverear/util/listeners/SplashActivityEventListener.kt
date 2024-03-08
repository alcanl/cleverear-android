package com.alcanl.cleverear.util.listeners

import android.util.Log
import com.ark.EventHandler
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService

class SplashActivityEventListener(private val mEventHandler: EventHandler,
    private val mThreadPool: ExecutorService) {
    private lateinit var mListener: Thread
    fun startEventListener()
    {

        mThreadPool.execute {
            try {
                mListener = Thread.currentThread()
                while (!mListener.isInterrupted) {
                    val event = mEventHandler.event
                    Log.e("eventSplash", event.data)

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