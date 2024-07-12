package com.alcanl.cleverear.fitting

import android.annotation.SuppressLint
import android.util.Log
import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.alcanl.cleverear.entity.HearingAid
import com.ark.ArkException
import com.ark.Parameter
import com.ark.ParameterSpace
import com.ark.Product

const val VOLUME_RETENTION_ENABLE = "X_VolumeRetention_Enable"
const val MEMORY_RETENTION_ENABLE = "X_MemoryRetention_Enable"
const val WDRC_ENABLE = "X_WDRC_Enable"
const val EQ_ENABLE = "X_EQ_Enable"
const val WDRC_CROSSOVER_FREQUENCY = "X_WDRC_CrossoverFrequency[%d]"
const val EQ_CHANNEL_GAIN = "X_EQ_ChannelGain_dB[%d]"
const val EQ_CROSSOVER_FREQUENCY = "X_EQ_CrossoverFrequency[%d]"
const val WDRC_EXPANSION_THRESHOLD = "X_WDRC_ExpansionThreshold[%d]"
const val FEEDBACK_CANCELER_ENABLE = "X_FBC_Enable"
const val WDRC_NOISE_REDUCTION_ENABLE = "X_WDRC_INR_Enable[%d]"
const val GAP_DEVICE_NAME = "X_RF_GAPDeviceName%d"
const val WIDE_BAND_GAIN = "X_LIM_TargetGain_dB"

private fun Product.isSystemParameter(parameterName: String) : Boolean
{
    return this.systemMemory.parameters.containsId(parameterName)
}
@SuppressLint("DefaultLocale")
private fun Product.setFittingValues(bound: Int, amount: Int, parameterName: String)
{
    try {
        (0..< bound).forEach {
            this.memories.getItem(currentMemory.value()).parameters.getById(
                String.format(parameterName, it)).value += amount
        }

    } catch (ex: ArkException) {
        Log.e("WDRC_Increase", ex.message!!)
    }
}
private fun setParameter(parameter: Parameter, value: Any)
{
    if (value is Int && parameter.isValidIntValue(value))
        parameter.value = value
    else if (value is Double && parameter.isValidDoubleValue(value))
        parameter.doubleValue = value
    else if (value is Boolean && parameter.isValidBooleanValue(value))
        parameter.isBooleanValue = value
    else
        throw ArkException("Invalid Parameter Name And Value Combination")

}
fun Product.setParameter(parameterName: String, value: Any)
{
    if (isSystemParameter(parameterName)) {
        setParameter(systemMemory.parameters.getById(parameterName), value)
    }
    else {
        setParameter(
            this.memories.getItem(currentMemory.value()).parameters.getById(parameterName),
            value
        )
    }
}
fun Product.enableEqualizer(memory: ParameterSpace)
{
    this.memories.getItem(memory.value()).parameters.getById(EQ_ENABLE).value = 1
}
fun Product.disableEqualizer(memory: ParameterSpace)
{
    this.memories.getItem(memory.value()).parameters.getById(EQ_ENABLE).value = 0
}
fun Product.enableEqualizerForAllMemories(hearingAid: HearingAid)
{
    (0..< hearingAid.numberOfMemories).forEach {
        enableEqualizer(ParameterSpace.value(it))
    }
}
fun Product.disableEqualizerForAllMemories(hearingAid: HearingAid)
{
    (0..< hearingAid.numberOfMemories).forEach {
        disableEqualizer(ParameterSpace.value(it))
    }
}
fun Product.readDoubleParameter(parameterName: String) : Double
{
    if (isSystemParameter(parameterName))
        return this.systemMemory.parameters.getById(parameterName).doubleValue

    return this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).doubleValue
}

fun Product.readIntParameter(parameterName: String) : Int
{
    if (isSystemParameter(parameterName))
        return this.systemMemory.parameters.getById(parameterName).value

    return this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).value
}
@SuppressLint("DefaultLocale")
fun Product.writeDeviceGapName(name: String)
{
    if (name.length > 22)
        throw UnsupportedOperationException("Name must be maximum 22 characters")

    val arr = name.toCharArray()
    val sb = StringBuilder()
    var gapNameParameterIndex = 0

    for (i in arr.indices) {
        val hex = Integer.toHexString(arr[i].code)
        sb.append(hex)

        if (i % 3 == 2 || i == arr.size - 1) {
            this.systemMemory.parameters.getById(String.format(GAP_DEVICE_NAME, gapNameParameterIndex))
                .value = Integer.parseInt(sb.toString())

            ++gapNameParameterIndex
            sb.clear()
        }
    }
}
fun Product.readAllParameters(hearingAid: HearingAid) : Boolean
{
    try {
        this.readParameters(ParameterSpace.kSystemNvmMemory)

        (0..<hearingAid.numberOfMemories).forEach {
            this.readParameters(ParameterSpace.value(it))
        }

        return true
    } catch (_: Throwable) { return false }
}
fun Product.increaseWDRCCrossOverFrequency(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.wdrcChannelCount - 1, amount, WDRC_CROSSOVER_FREQUENCY)
}
fun Product.decreaseWDRCCrossOverFrequency(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.wdrcChannelCount - 1, -1 * amount, WDRC_CROSSOVER_FREQUENCY)
}
fun Product.increaseEqualizerChannelGain(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.frequencyChannelCount, amount, EQ_CHANNEL_GAIN)
}
fun Product.decreaseEqualizerChannelGain(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.frequencyChannelCount, -1 * amount, EQ_CHANNEL_GAIN)
}
fun Product.increaseEqualizerCrossOverFrequency(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.frequencyChannelCount - 1, amount, EQ_CROSSOVER_FREQUENCY)
}
fun Product.decreaseEqualizerCrossOverFrequency(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.frequencyChannelCount - 1, -1 * amount, EQ_CROSSOVER_FREQUENCY)
}
fun Product.increaseWDRCExpansionThreshold(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.wdrcChannelCount, amount, WDRC_EXPANSION_THRESHOLD)
}
fun Product.decreaseWDRCExpansionThreshold(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.wdrcChannelCount, -1 * amount, WDRC_EXPANSION_THRESHOLD)
}
fun Product.enableFeedBackCanceler()
{
    this.memories.getItem(currentMemory.value()).parameters.getById(FEEDBACK_CANCELER_ENABLE).value = 1
}
fun Product.disableFeedBackCanceler()
{
    this.memories.getItem(currentMemory.value()).parameters.getById(FEEDBACK_CANCELER_ENABLE).value = 0
}
fun Product.burnChangesToMemory(memory: ParameterSpace)
{
    this.writeParameters(ParameterSpace.value(memory.value()))
    this.writeParameters(ParameterSpace.kSystemNvmMemory)
}
fun Product.burnChangesToAllMemories(hearingAid: HearingAid)
{
    (0..< hearingAid.numberOfMemories).forEach {
        this.writeParameters(ParameterSpace.value(it))
    }
    this.writeParameters(ParameterSpace.kSystemNvmMemory)
}
fun Product.enableWDRC(memory: ParameterSpace)
{
    this.memories.getItem(memory.value()).parameters.getById(WDRC_ENABLE).value = 1
}
fun Product.disableWDRC(memory: ParameterSpace)
{
    this.memories.getItem(memory.value()).parameters.getById(WDRC_ENABLE).value = 0
}
fun Product.enableWDRCForAllMemories(hearingAid: HearingAid)
{
    (0..< hearingAid.numberOfMemories).forEach {
        enableWDRC(ParameterSpace.value(it))
    }
}
fun Product.disableWDRCForAllMemories(hearingAid: HearingAid)
{
    (0..< hearingAid.numberOfMemories).forEach {
        disableWDRC(ParameterSpace.value(it))
    }
}
@SuppressLint("DefaultLocale")
fun Product.enableNoiseReduction(hearingAidDTO: HearingAidDTO)
{
    (0..< hearingAidDTO.wdrcChannelCount).forEach {
        this.memories.getItem(currentMemory.value()).parameters.getById(String.format(
            WDRC_NOISE_REDUCTION_ENABLE, it))
            .value = 1
    }
}
@SuppressLint("DefaultLocale")
fun Product.disableNoiseReduction(hearingAidDTO: HearingAidDTO)
{
    (0..< hearingAidDTO.wdrcChannelCount).forEach {
        this.memories.getItem(currentMemory.value()).parameters.getById(String.format(
            WDRC_NOISE_REDUCTION_ENABLE, it))
            .value = 0
    }
}
fun Product.isEqualizerEnabled() : Boolean
{
    return this.memories.getItem(currentMemory.value()).parameters.getById(EQ_ENABLE).value == 1
}
fun Product.isFeedbackCancelerEnabled() : Boolean
{
    return this.memories.getItem(currentMemory.value()).parameters.getById(FEEDBACK_CANCELER_ENABLE).value == 1
}
@SuppressLint("DefaultLocale")
fun Product.isNoiseReductionEnabled(hearingAidDTO: HearingAidDTO) : Boolean
{
    return (0..< hearingAidDTO.wdrcChannelCount).all {
        this.memories.getItem(currentMemory.value()).parameters.getById(String.format(
            WDRC_NOISE_REDUCTION_ENABLE, it)).value == 1
    }
}

fun Product.setWideBandGain(value: Int)
{
    setParameter(WIDE_BAND_GAIN, value)
}
fun Product.isMemoryRetentionEnabled() : Boolean
{
    return readIntParameter(MEMORY_RETENTION_ENABLE) == 1
}
fun Product.isVolumeRetentionEnabled() : Boolean
{
    return readIntParameter(VOLUME_RETENTION_ENABLE) == 1
}
fun Product.enableMemoryRetention()
{
    setParameter(MEMORY_RETENTION_ENABLE, 1)
}
fun Product.enableVolumeRetention()
{
    setParameter(VOLUME_RETENTION_ENABLE, 1)
}
fun Product.disableMemoryRetention()
{
    setParameter(MEMORY_RETENTION_ENABLE, 0)
}
fun Product.disableVolumeRetention()
{
    setParameter(VOLUME_RETENTION_ENABLE, 0)
}
fun Product.getWideBandGainValue() : Int
{
    return readIntParameter(WIDE_BAND_GAIN)
}
fun Product.getEqualizerBassValue(hearingAidDTO: HearingAidDTO) : Int
{
    TODO("Not Implemented Yet")
}
fun Product.getEqualizerMidValue(hearingAidDTO: HearingAidDTO) : Int
{
    TODO("Not Implemented Yet")
}
fun Product.getEqualizerTrebleValue(hearingAidDTO: HearingAidDTO) : Int
{
    TODO("Not Implemented Yet")
}






