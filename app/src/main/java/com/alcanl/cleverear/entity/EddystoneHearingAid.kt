package com.alcanl.cleverear.entity

import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM

data class EddystoneHearingAid(var tlmVersion : Int, var batteryVoltage : Int, var temperature : Double,
                               var advertisementCount : Long, var elapsedTime : Long, var stepCount : Long,
                               var isFallen : Boolean, var distance : Long, var isFallenSecond : Boolean,
                               var isTrackingActive : Boolean, var macAddress: String) {

    constructor() : this(
        0,0, 0.0, 0L, 0L, 0L,
        false, 0L, false, false, "")

    private fun setStepCount(stepCountFirstData: UInt, stepCountSecondData: UInt)
    {
        stepCount += ((stepCountFirstData shl 8) or stepCountSecondData).toLong()
    }

    private fun setTemperatureData(tempFirstData: String, tempSecondData: String)
    {
        temperature = (tempFirstData + tempSecondData).toLong(16).toDouble() / 1000
    }

    fun reSetEddyStoneProperties(eddystoneTLM: EddystoneTLM)
    {
        setEddystoneProperties(eddystoneTLM, macAddress)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun setEddystoneProperties(eddystoneTLM: EddystoneTLM, mac: String)
    {
        macAddress = mac
        tlmVersion = eddystoneTLM.tlmVersion
        batteryVoltage = eddystoneTLM.batteryVoltage
        advertisementCount = eddystoneTLM.advertisementCount
        elapsedTime = eddystoneTLM.elapsedTime
        isFallen = eddystoneTLM.data[10].toUInt() == 1u
        isFallenSecond = eddystoneTLM.data[11].toUInt() == 1u
        setStepCount(eddystoneTLM.data[8].toUInt(), eddystoneTLM.data[9].toUInt())
        setTemperatureData(eddystoneTLM.data[6].toHexString(), eddystoneTLM.data[7].toHexString())
    }
}