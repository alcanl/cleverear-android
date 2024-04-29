package com.alcanl.cleverear.api

import com.alcanl.cleverear.api.library.LibraryInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IEarTechnicLibraryService {
    @GET("/model/library/info")
    fun findLibraryByHearingAidModelName(@Query("name")modelName: String) : Call<LibraryInfo>

}