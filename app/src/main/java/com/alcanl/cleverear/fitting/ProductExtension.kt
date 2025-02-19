package com.alcanl.cleverear.fitting

import android.annotation.SuppressLint
import android.util.Log
import com.alcanl.cleverear.api.aid.HearingAidDTO
import com.alcanl.cleverear.entity.HearingAid
import com.ark.ArkException
import com.ark.ParameterSpace
import com.ark.Product

private const val VOLUME_RETENTION_ENABLE = "X_VolumeRetention_Enable"
private const val MEMORY_RETENTION_ENABLE = "X_MemoryRetention_Enable"
private const val EQ_ENABLE = "X_EQ_Enable"
private const val EQ_CHANNEL_GAIN = "X_EQ_ChannelGain_dB[%d]"
private const val FEEDBACK_CANCELER_ENABLE = "X_FBC_Enable"
private const val WIDE_BAND_GAIN = "X_LIM_TargetGain_dB"

private fun Product.isSystemParameter(parameterName: String) : Boolean
{
    return this.systemMemory.parameters.containsId(parameterName)
}

@SuppressLint("DefaultLocale")
private fun Product.getEqualizerValue(from: Int, to: Int) : Int
{
    var sum = 0
    (from..< to).forEach {
        sum += this.readIntParameter(String.format(EQ_CHANNEL_GAIN, it))
    }

    val result = sum / (to - from) + 18
    return if (result > 36) result - 18 else result
}

@SuppressLint("DefaultLocale")
private fun Product.setEqualizerValue(from: Int, to: Int, value: Int, equalizerValues: List<Int>)
{
    try {
        (from..< to).forEach {
            this.memories.getItem(currentMemory.value()).parameters.getById(
                String.format(EQ_CHANNEL_GAIN, it)).value = if (value > equalizerValues[it] || value < equalizerValues[it])
                    equalizerValues[it] - 18 else value - 18
        }

    } catch (ex: ArkException) {
        Log.e(Product::class.simpleName, ex.message!!)
    }
}

fun Product.setParameter(parameterName: String, value: Int)
{
    if (isSystemParameter(parameterName))
        this.systemMemory.parameters.getById(parameterName).value = value
    else
        this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).value = value
}
fun Product.enableEqualizer(memory: ParameterSpace)
{
    this.memories.getItem(memory.value()).parameters.getById(EQ_ENABLE).value = 1
}
fun Product.disableEqualizer(memory: ParameterSpace)
{
    this.memories.getItem(memory.value()).parameters.getById(EQ_ENABLE).value = 0
}

fun Product.readIntParameter(parameterName: String) : Int
{
    if (isSystemParameter(parameterName))
        return this.systemMemory.parameters.getById(parameterName).value

    return this.memories.getItem(currentMemory.value()).parameters.getById(parameterName).value
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

fun Product.setEqualizerBassValue(dto: HearingAidDTO, equalizerValues: List<Int>, value: Int)
{
    when (dto.wdrcChannelCount) {
        49 -> this.setEqualizerValue(0, 9, value, equalizerValues)
        36 -> this.setEqualizerValue(0, 9, value, equalizerValues)
        24 -> this.setEqualizerValue(0, 9, value, equalizerValues)
        16 -> this.setEqualizerValue(0, 6, value, equalizerValues)
        12 -> this.setEqualizerValue(0, 6, value, equalizerValues)
    }
}

fun Product.setEqualizerMidValue(dto: HearingAidDTO, equalizerValues: List<Int>, value: Int)
{
    when (dto.wdrcChannelCount) {
        49 -> this.setEqualizerValue(9, 19, value, equalizerValues)
        36 -> this.setEqualizerValue(9, 19, value, equalizerValues)
        24 -> this.setEqualizerValue(9, 17, value, equalizerValues)
        16 -> this.setEqualizerValue(6, 11, value, equalizerValues)
        12 -> this.setEqualizerValue(6, 9, value, equalizerValues)
    }
}

fun Product.setEqualizerTrebleValue(dto: HearingAidDTO, equalizerValues: List<Int>, value: Int)
{
    when (dto.wdrcChannelCount) {
        49 -> this.setEqualizerValue(19, 49, value, equalizerValues)
        36 -> this.setEqualizerValue(19, 36, value, equalizerValues)
        24 -> this.setEqualizerValue(17, 24, value, equalizerValues)
        16 -> this.setEqualizerValue(11, 16, value, equalizerValues)
        12 -> this.setEqualizerValue(9, 12, value, equalizerValues)
    }
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

fun Product.isEqualizerEnabled() : Boolean
{
    return this.memories.getItem(currentMemory.value()).parameters.getById(EQ_ENABLE).value == 1
}
fun Product.isFeedbackCancelerEnabled() : Boolean
{
    return this.memories.getItem(currentMemory.value()).parameters.getById(FEEDBACK_CANCELER_ENABLE).value == 1
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
    return when (hearingAidDTO.wdrcChannelCount) {
        49 -> this.getEqualizerValue(0, 9)
        36 -> this.getEqualizerValue(0, 9)
        24 -> this.getEqualizerValue(0, 9)
        16 -> this.getEqualizerValue(0, 6)
        else -> this.getEqualizerValue(0, 6)
    }
}
fun Product.getEqualizerMidValue(hearingAidDTO: HearingAidDTO) : Int
{
    return when (hearingAidDTO.wdrcChannelCount) {
        49 -> this.getEqualizerValue(9, 19)
        36 -> this.getEqualizerValue(9, 19)
        24 -> this.getEqualizerValue(9, 17)
        16 -> this.getEqualizerValue(6, 11)
        else -> this.getEqualizerValue(6, 9)
    }
}
fun Product.getEqualizerTrebleValue(hearingAidDTO: HearingAidDTO) : Int
{
    return when (hearingAidDTO.wdrcChannelCount) {
        49 -> this.getEqualizerValue(19, 49)
        36 -> this.getEqualizerValue(19, 36)
        24 -> this.getEqualizerValue(17, 24)
        16 -> this.getEqualizerValue(11, 16)
        else -> this.getEqualizerValue(9, 12)
    }
}







