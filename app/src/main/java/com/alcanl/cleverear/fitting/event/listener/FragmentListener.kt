package com.alcanl.cleverear.fitting.event.listener

import com.alcanl.cleverear.fitting.event.EventObject
import java.util.EventListener

interface FragmentListener: EventListener {
    fun onEventReceive(event: EventObject)
}