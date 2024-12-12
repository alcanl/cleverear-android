package com.alcanl.cleverear.entity

data class EddystoneHearingAid(var tlmVersion : Int, var batteryVoltage : Int,
                               var temperature : Double, var advertisementCount : Long,
                               var elapsedTime : Long, var stepCount : Long,
                               var isFallen : Boolean, var distance : Long,
                               var isDefault : Boolean) {

    constructor() : this(0, 0, 0.0, 0L, 0L,
                        0L, false, 0L, false)

    fun setStepCount(stepCountFirstData: Byte, stepCountSecondData: Byte)
    {

    }

    fun setTemperatureData(stepCountFirstData: Byte, stepCountSecondData: Byte)
    {

    }
}