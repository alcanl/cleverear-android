package com.alcanl.cleverear.fitting.event

import java.util.EventObject
import java.util.UUID

class FittingEvent(source: Any?) : EventObject(source) {
    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
}