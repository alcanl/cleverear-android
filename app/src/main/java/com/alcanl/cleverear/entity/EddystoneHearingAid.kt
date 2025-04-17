package com.alcanl.cleverear.entity

import android.util.Log
import com.alcanl.cleverear.fragment.BeaconFragment
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

    private fun setTemperatureData(tempFirstData: UInt, tempSecondData: UInt)
    {
        temperature = (((tempFirstData shl 8) or tempSecondData).toInt()).toDouble() / 1000
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
        temperature = eddystoneTLM.beaconTemperature.toDouble()
        batteryVoltage = eddystoneTLM.batteryVoltage
        advertisementCount = eddystoneTLM.advertisementCount
        elapsedTime = eddystoneTLM.elapsedTime
        isFallen = eddystoneTLM.data[10].toHexString().toBoolean()
        isFallenSecond = eddystoneTLM.data[11].toHexString().toBoolean()
        setStepCount(eddystoneTLM.data[8].toUInt(), eddystoneTLM.data[9].toUInt())
        setTemperatureData(eddystoneTLM.data[6].toUInt(), eddystoneTLM.data[7].toUInt())

        val version = eddystoneTLM.tlmVersion
        Log.d(BeaconFragment::class.simpleName, version.toString())

        val voltage = eddystoneTLM.batteryVoltage
        Log.d(BeaconFragment::class.simpleName, voltage.toString())

        val temperature = eddystoneTLM.beaconTemperature
        Log.d(BeaconFragment::class.simpleName, temperature.toString())

        val count = eddystoneTLM.advertisementCount
        Log.d(BeaconFragment::class.simpleName, count.toString())

        val elapsed = eddystoneTLM.elapsedTime
        Log.d(BeaconFragment::class.simpleName, elapsed.toString())

    }
}