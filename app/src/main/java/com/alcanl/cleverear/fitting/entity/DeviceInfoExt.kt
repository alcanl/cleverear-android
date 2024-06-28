package com.alcanl.cleverear.fitting.entity

import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.alcanl.cleverear.fitting.isEqualizerEnabled
import com.alcanl.cleverear.fitting.isFeedbackCancelerEnabled
import com.alcanl.cleverear.fitting.isNoiseReductionEnabled
import com.ark.DeviceInfo
import com.ark.Library
import com.ark.Product

data class DeviceInfoExt(val product: Product?, val deviceInfo: DeviceInfo?,
                         val hearingAidDTO: HearingAidDTO?, val library: Library?,
                         var isInitFinished: Boolean = false) {

    constructor() : this(null, null, null, null)

    fun getDeviceMac(): String = product?.deviceMACAddress ?: ""
    fun getDeviceModelName() : String = hearingAidDTO?.modelName ?: ""
    fun getDeviceFirmwareId() : String = deviceInfo?.firmwareId ?: ""
    fun getDeviceFirmwareVersion() : String = deviceInfo?.firmwareVersion ?: ""
    fun getDeviceModelNumber() : String = deviceInfo?.serialId?.toString() ?: ""
    fun getDeviceLibraryId() : String = if (deviceInfo == null) "" else Integer.toHexString(deviceInfo.libraryId)
    fun isEqualizerEnabled() = product?.isEqualizerEnabled() ?: false
    fun isNoiseReductionEnabled() = product?.isNoiseReductionEnabled(hearingAidDTO!!) ?: false
    fun isFeedbackCancelerEnabled() = product?.isFeedbackCancelerEnabled() ?: false

}