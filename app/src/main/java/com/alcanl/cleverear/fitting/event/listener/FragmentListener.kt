package com.alcanl.cleverear.fitting.event.listener

import com.alcanl.cleverear.fitting.event.FittingEvent
import java.util.EventListener

interface FragmentListener: EventListener {
    fun onEventReceive(event: FittingEvent)
}