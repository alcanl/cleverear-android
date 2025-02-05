package com.alcanl.cleverear.entity

import android.util.Log
import com.alcanl.cleverear.fragment.BeaconFragment
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM

data class EddystoneHearingAid(
    var tlmVersion : Int, var batteryVoltage : Int, var temperature : Double,
    var advertisementCount : Long, var elapsedTime : Long, var stepCount : Long,
    var isFallen : Boolean, var distance : Long, var isDefault : Boolean,
    var isTrackingActive : Boolean, var macAddress: String) {

    constructor() : this(
        0,0, 0.0, 0L, 0L, 0L,
        false, 0L, false, false, "")

    private fun setStepCount(stepCountFirstData: Byte, stepCountSecondData: Byte)
    {

    }

    private fun setTemperatureData(stepCountFirstData: Byte, stepCountSecondData: Byte)
    {

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
        isFallen = eddystoneTLM.data[38].toHexString().toBoolean()
        isDefault = eddystoneTLM.data[39].toHexString().toBoolean()
        setStepCount(eddystoneTLM.data[36], eddystoneTLM.data[37])
        setTemperatureData(eddystoneTLM.data[34], eddystoneTLM.data[35])

        //it.data.forEach { byte -> Log.d("raw data", byte.toString()) }
        //6 - 15 kapalı
        //ikincisi left shift + or
        //34 35 sıcaklık
        //36 37 step
        //38 free fall bool
        //39 bool defa 0

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