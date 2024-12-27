package com.alcanl.cleverear.util.listeners.event

import com.alcanl.cleverear.entity.DiscoveredDeviceInfo
import java.util.EventObject
import java.util.UUID
import kotlin.properties.Delegates

class DeviceFoundEvent(source: Any?) : EventObject(source) {
    val serialVersionUUID : UUID by lazy {
        UUID.randomUUID()
    }
    var hearingAid by Delegates.notNull<DiscoveredDeviceInfo>()

}