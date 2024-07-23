package com.alcanl.cleverear.api.library

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.util.Base64
import java.util.Objects

@Serializable
data class LibraryDTO(
    @SerializedName("libraryName") val libraryId : String? = null,
    @SerializedName("libraryData") val libraryFile : String? = null
) {
    override fun equals(other: Any?): Boolean
    {
        return other is LibraryDTO && libraryId == other.libraryId
                && libraryFile == other.libraryFile
    }

    override fun hashCode(): Int
    {
        return Objects.hash(libraryId, libraryFile)
    }

    override fun toString(): String
    {
        return String.format("Id: %s, Data Size: %s", libraryId, libraryFile)
    }
    fun convertLibraryData(): ByteArray = Base64.getDecoder().decode(libraryFile)

}

