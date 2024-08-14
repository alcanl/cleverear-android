package com.alcanl.cleverear.util.listeners.event

import java.util.EventObject
import java.util.UUID
import kotlin.properties.Delegates

class BatteryEvent(source: Any?) : EventObject(source) {
    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
    var batteryData by Delegates.notNull<Int>()
    lateinit var eventSender : String
}
