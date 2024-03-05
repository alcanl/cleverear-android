package com.alcanl.cleverear.util.listeners

import android.util.Log
import com.ark.EventHandler
import java.lang.NullPointerException
import kotlin.concurrent.thread

class SplashActivityEventListener(private val mEventHandler: EventHandler) {
    private lateinit var mListener: Thread
    fun startEventListener()
    {

        mListener = thread(isDaemon = true, start = true) {
            try {
                do {
                    val event = mEventHandler.event
                    Log.e("event", event.data)

                } while (!mListener.isInterrupted)

            } catch (ex: NullPointerException) {
                Log.e("null", ex.message!!)
            } catch (ex: InterruptedException) {
                Log.e("thread", ex.message!!)
            }
        }
    }
    fun stopListener()
    {
        mListener.interrupt()
    }
}