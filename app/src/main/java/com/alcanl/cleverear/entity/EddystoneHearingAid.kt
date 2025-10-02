package com.alcanl.cleverear.entity

import android.content.Context
import com.alcanl.cleverear.util.readStepCount
import com.alcanl.cleverear.util.saveStepCount
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM

data class EddystoneHearingAid(var tlmVersion : Int, var batteryVoltage : Int, @Volatile var temperature : Double,
                               var advertisementCount : Long, var elapsedTime : Long, @Volatile var stepCount : Long,
                               var isFallen : Boolean, var distance : Long, var isFallenSecond : Boolean,
                               var isTrackingActive : Boolean, var macAddress: String, var deviceName: String) {

    constructor() : this(
        0,0, 0.0, 0L, 0L, 0L,
        false, 0L, false, false, "", "")
    private fun setStepCount(stepCountFirstData: String, stepCountSecondData: String, context: Context)
    {
        if (readStepCount(context, macAddress) != (stepCountFirstData + stepCountSecondData).toLong(16))
        {
            stepCount =  readStepCount(context, macAddress) + (stepCountFirstData + stepCountSecondData).toLong(16)
            saveStepCount(stepCount, context, macAddress)
        }
    }
    private fun setTemperatureData(tempFirstData: String, tempSecondData: String)
    {
        temperature = (tempFirstData + tempSecondData).toLong(16).toDouble() / 1000
    }
    fun reSetEddyStoneProperties(eddystoneTLM: EddystoneTLM, context: Context)
    {
        setEddystoneProperties(eddystoneTLM, macAddress, deviceName, context)
    }
    fun isLowTemperature() = temperature < 35.0 && temperature != 0.0
    fun isHighTemperature() = temperature > 38.0
    @OptIn(ExperimentalStdlibApi::class)
    fun setEddystoneProperties(eddystoneTLM: EddystoneTLM, mac: String, name: String, context: Context)
    {
        deviceName = name
        macAddress = mac
        tlmVersion = eddystoneTLM.tlmVersion
        batteryVoltage = eddystoneTLM.batteryVoltage
        advertisementCount = eddystoneTLM.advertisementCount
        elapsedTime = eddystoneTLM.elapsedTime
        isFallen = eddystoneTLM.data[10].toUInt() == 1u
        isFallenSecond = eddystoneTLM.data[11].toUInt() == 1u
        setStepCount(eddystoneTLM.data[8].toHexString(), eddystoneTLM.data[9].toHexString(), context)
        setTemperatureData(eddystoneTLM.data[6].toHexString(), eddystoneTLM.data[7].toHexString())

    }
}