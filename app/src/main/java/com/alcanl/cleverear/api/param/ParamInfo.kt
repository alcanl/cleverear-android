package com.alcanl.cleverear.api.param

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.util.Base64
import java.util.Objects

@Serializable
data class ParamInfo(
    val paramId : String? = null,
    @SerializedName("paramData") val paramFile: String? = null,
    @SerializedName("libraryName")val libraryId: String? = null
) {

    override fun equals(other: Any?): Boolean
    {
        return other is ParamInfo && paramId == other.paramId
                && libraryId == other.libraryId
    }

    override fun hashCode(): Int
    {
        return Objects.hash(paramId, libraryId)
    }

    override fun toString(): String
    {
        return String.format("Id: %s, Data Size: %s", paramId, paramFile?.length)
    }
    fun convertParamData(): ByteArray = Base64.getDecoder().decode(paramFile)
}