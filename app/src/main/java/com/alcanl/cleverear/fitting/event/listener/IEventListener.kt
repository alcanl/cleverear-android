package com.alcanl.cleverear.fitting.event.listener

import java.util.EventListener
import java.util.EventObject

interface IEventListener: EventListener {
    fun onEventReceive(event: EventObject)
}