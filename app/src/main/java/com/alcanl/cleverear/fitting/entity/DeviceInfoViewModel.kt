package com.alcanl.cleverear.fitting.entity

import androidx.lifecycle.ViewModel

data class DeviceInfoViewModel(var isEqualizerEnabled: Boolean, var isFeedBackCancelerEnabled: Boolean,
                               var isNoiseReductionEnabled: Boolean, var equalizerTrebleValue: Int,
                               var equalizerMidValue: Int, var equalizerBassValue: Int, var wideRangeBandGainValue: Int,
                               var isMemoryRetentionEnabled: Boolean, var isVolumeRetentionEnabled: Boolean): ViewModel() {


    constructor() : this(false, false, false,
        0, 0, 0, 0, false, false)

    fun initializeDeviceInfoViewModel(deviceInfoExt: DeviceInfoExt)
    {
        isEqualizerEnabled = deviceInfoExt.isEqualizerEnabled()
        isFeedBackCancelerEnabled = deviceInfoExt.isFeedbackCancelerEnabled()
        isMemoryRetentionEnabled = deviceInfoExt.isMemoryRetentionEnabled()
        isVolumeRetentionEnabled = deviceInfoExt.isVolumeRetentionEnabled()
        wideRangeBandGainValue = deviceInfoExt.getProductWideBandValue()
        equalizerMidValue = deviceInfoExt.getProductEqualizerMidValue()
        equalizerTrebleValue = deviceInfoExt.getProductEqualizerTrebleValue()
        equalizerBassValue = deviceInfoExt.getProductEqualizerBassValue()
    }
}
