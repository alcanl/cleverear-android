package com.alcanl.cleverear.api.dto

import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable
data class HearingAidDTO(val modelName: String, val modelNumber: Int, val libraryId: String,
                         val defaultParamId: String, val activeParamId: String,
                         val wdrcChannelCount: Int, val frequencyChannelCount: Int) {
    override fun equals(other: Any?): Boolean
    {
        return other is HearingAidDTO && other.modelName == modelName && other.modelNumber == modelNumber
    }

    override fun hashCode(): Int
    {
        return Objects.hash(modelName, modelNumber)
    }

    override fun toString(): String
    {
        return modelName
    }
}