package com.alcanl.cleverear.entity

import java.util.Objects

data class DiscoveredDeviceInfo(val address: String, val name: String, val rssi: Int, val manufacturerData: String) {
    override fun equals(other: Any?): Boolean
    {
        return other is DiscoveredDeviceInfo && address == other.address && name == other.name
    }
    override fun hashCode(): Int
    {
        return Objects.hashCode(address)
    }

    override fun toString(): String
    {
        return name
    }
}


