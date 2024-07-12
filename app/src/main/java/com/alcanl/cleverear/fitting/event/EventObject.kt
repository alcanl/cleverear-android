package com.alcanl.cleverear.fitting.event

import java.util.EventObject
import java.util.UUID

class EventObject(source: Any?) : EventObject(source) {
    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
}