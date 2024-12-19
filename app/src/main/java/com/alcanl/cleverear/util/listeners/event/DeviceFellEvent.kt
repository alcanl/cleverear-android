package com.alcanl.cleverear.util.listeners.event

import com.alcanl.cleverear.util.EarSide
import java.util.EventObject
import java.util.UUID
import kotlin.properties.Delegates

class DeviceFellEvent(source: Any?) : EventObject(source) {

    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
    var earSide by Delegates.notNull<EarSide>()
    var serialNumber by Delegates.notNull<String>()

    lateinit var eventSender : String
}
