package com.alcanl.cleverear.fitting.event.listener

import com.alcanl.cleverear.fitting.event.InSituEvent
import java.util.EventListener

interface InSituFragmentListener : EventListener{
    fun onEventReceive(event: InSituEvent)
}