package com.alcanl.cleverear.fitting.event

import com.alcanl.cleverear.entity.HearingAid
import java.util.EventObject
import java.util.UUID

class DisconnectEvent(source: Any?, val hearingAid: HearingAid?) : EventObject(source) {
    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
}