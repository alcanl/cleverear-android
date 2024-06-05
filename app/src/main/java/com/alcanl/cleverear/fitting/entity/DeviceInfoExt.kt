package com.alcanl.cleverear.fitting.entity

import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.ark.DeviceInfo
import com.ark.Library
import com.ark.Product

data class DeviceInfoExt(val product: Product?, val deviceInfo: DeviceInfo?,
                        val hearingAidDTO: HearingAidDTO?, val library: Library?) {

    constructor() : this(null, null, null, null)
    fun getDeviceMac(): String = product?.deviceMACAddress ?: ""
    fun getDeviceModelName() : String = hearingAidDTO?.modelName ?: ""
    fun getDeviceFirmwareId() : String = deviceInfo?.firmwareId ?: ""
    fun getDeviceFirmwareVersion() : String = deviceInfo?.firmwareVersion ?: ""
    fun getDeviceModelNumber() : String = deviceInfo?.serialId?.toString() ?: ""
    fun getDeviceLibraryId() : String = if (deviceInfo == null) "" else Integer.toHexString(deviceInfo.libraryId)
}