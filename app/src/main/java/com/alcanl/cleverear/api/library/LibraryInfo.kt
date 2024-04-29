package com.alcanl.cleverear.api.library

import kotlinx.serialization.Serializable

@Serializable
class LibraryInfo {
    var libraryId : String? = null
    var libraryFile : ByteArray? = null
}