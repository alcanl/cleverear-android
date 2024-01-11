package com.alcanl.cleverear.entity

data class DiscoveredDeviceInfo(val address: String, val name: String, val rssi: Int, val manufacturerData: String, val isBond: Boolean = false)


