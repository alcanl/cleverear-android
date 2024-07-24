package com.alcanl.cleverear.fitting.entity

import androidx.lifecycle.ViewModel

data class DeviceInfoViewModel(var isEqualizerEnabled: Boolean, var isFeedBackCancelerEnabled: Boolean,
                               var equalizerTrebleValue: Int, var equalizerMidValue: Int,
                               var equalizerBassValue: Int, var wideRangeBandGainValue: Int,
                               var isMemoryRetentionEnabled: Boolean, var isVolumeRetentionEnabled: Boolean,
                               var deviceModelNumber: String): ViewModel() {


    constructor(deviceInfoExt: DeviceInfoExt) : this(
        isEqualizerEnabled = deviceInfoExt.isEqualizerEnabled(),
        isFeedBackCancelerEnabled = deviceInfoExt.isFeedbackCancelerEnabled(),
        equalizerTrebleValue = deviceInfoExt.getProductEqualizerTrebleValue(),
        equalizerMidValue = deviceInfoExt.getProductEqualizerMidValue(),
        equalizerBassValue = deviceInfoExt.getProductEqualizerBassValue(),
        wideRangeBandGainValue = deviceInfoExt.getProductWideBandValue(),
        isMemoryRetentionEnabled = deviceInfoExt.isMemoryRetentionEnabled(),
        isVolumeRetentionEnabled = deviceInfoExt.isVolumeRetentionEnabled(),
        deviceModelNumber = deviceInfoExt.getDeviceModelNumber()
    )
}
