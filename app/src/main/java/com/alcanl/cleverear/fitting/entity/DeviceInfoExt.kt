package com.alcanl.cleverear.fitting.entity

import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.ark.DeviceInfo
import com.ark.Library
import com.ark.Product

data class DeviceInfoExt(val product: Product, val deviceInfo: DeviceInfo,
                        val hearingAidDTO: HearingAidDTO, val library: Library) {

    fun getDeviceMac() = product.deviceMACAddress
    fun getDeviceModelName() = hearingAidDTO.modelName
    fun getDeviceFirmwareId() = deviceInfo.firmwareId
    fun getDeviceFirmwareVersion() = deviceInfo.firmwareVersion
    fun getDeviceModelNumber() = deviceInfo.serialId.toString()
    fun getDeviceLibraryId() = Integer.toHexString(deviceInfo.libraryId)
}