package com.alcanl.cleverear.api.equalizer

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable
data class EqualizerValuesDTO(@SerializedName("equalizerValuesId") val id: Long,
                              @SerializedName("values") val valueList: MutableList<Int>,
                              val equalizerValuesName: String) {

    override fun equals(other: Any?): Boolean
    {
        return other is EqualizerValuesDTO && id == other.id
                && equalizerValuesName == other.equalizerValuesName
    }

    override fun hashCode(): Int
    {
        return Objects.hash(id, equalizerValuesName)
    }

    override fun toString(): String
    {
        return String.format("Id: %s, Hearing Aid Model: %s", id, equalizerValuesName)
    }
}