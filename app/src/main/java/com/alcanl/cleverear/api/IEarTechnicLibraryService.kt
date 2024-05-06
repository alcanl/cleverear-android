package com.alcanl.cleverear.api

import com.alcanl.cleverear.api.library.LibraryInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IEarTechnicLibraryService {
    @GET("library/model/info")
    fun findLibraryByHearingAidModelName(@Query("name")modelName: String) : Call<LibraryInfo>

    @GET("library")
    fun findLibraryByLibraryId(@Query("id")libraryId: String) : Call<LibraryInfo>

}