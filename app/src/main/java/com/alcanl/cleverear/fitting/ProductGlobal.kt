package com.alcanl.cleverear.fitting

import android.annotation.SuppressLint
import android.util.Log
import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.alcanl.cleverear.entity.HearingAid
import com.ark.ArkException
import com.ark.Parameter
import com.ark.ParameterSpace
import com.ark.Product

const val WDRC_ENABLE = "X_WDRC_Enable"
const val WDRC_CROSSOVER_FREQUENCY = "X_WDRC_CrossoverFrequency[%d]"
const val EQ_CHANNEL_GAIN = "X_EQ_ChannelGain_dB[%d]"
const val EQ_CROSSOVER_FREQUENCY = "X_EQ_CrossoverFrequency[%d]"
const val WDRC_EXPANSION_THRESHOLD = "X_WDRC_ExpansionThreshold[%d]"
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
        writeParameters(ParameterSpace.kSystemNvmMemory)
    }
    else {
        setParameter(
            this.memories.getItem(currentMemory.value()).parameters.getById(parameterName),
            value
        )
        writeParameters(currentMemory)
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
fun Product.increaseExpansionThreshold(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.wdrcChannelCount, amount, WDRC_EXPANSION_THRESHOLD)
}
fun Product.decreaseExpansionThreshold(dto: HearingAidDTO, amount: Int)
{
    setFittingValues(dto.wdrcChannelCount, -1 * amount, WDRC_EXPANSION_THRESHOLD)
}


