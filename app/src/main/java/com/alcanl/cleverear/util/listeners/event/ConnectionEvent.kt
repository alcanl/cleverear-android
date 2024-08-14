package com.alcanl.cleverear.util.listeners.event

import java.util.EventObject
import java.util.UUID
import kotlin.properties.Delegates

class ConnectionEvent(source: Any?) : EventObject(source) {
    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
    var connectionData by Delegates.notNull<Int>()
}