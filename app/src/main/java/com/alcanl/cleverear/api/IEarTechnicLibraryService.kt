package com.alcanl.cleverear.api

import com.alcanl.cleverear.api.library.LibraryInfo
import com.alcanl.cleverear.api.param.ParamInfo
import com.alcanl.cleverear.util.API_ACTIVE_PARAM_DATA_GET_BY_USER
import com.alcanl.cleverear.util.API_LIB_INFO_GET_BY_HA_URL
import com.alcanl.cleverear.util.API_LIB_INFO_GET_BY_LIB_ID_URL
import com.alcanl.cleverear.util.API_PARAM_DEFAULT_DATA_GET_BY_HA_NAME
import com.alcanl.cleverear.util.API_PARAM_DEFAULT_DATA_GET_BY_HA_NUM
import com.alcanl.cleverear.util.API_QUERY_EMAIL
import com.alcanl.cleverear.util.API_QUERY_ID
import com.alcanl.cleverear.util.API_QUERY_NAME
import com.alcanl.cleverear.util.API_QUERY_NUM
import com.alcanl.cleverear.util.API_QUERY_PASS
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IEarTechnicLibraryService {
    @GET(API_LIB_INFO_GET_BY_HA_URL)
    fun findLibraryByHearingAidModelName(@Query(API_QUERY_NAME)modelName: String) : Call<LibraryInfo>

    @GET(API_LIB_INFO_GET_BY_LIB_ID_URL)
    fun findLibraryByLibraryId(@Query(API_QUERY_ID)id: String) : Call<LibraryInfo>

    @GET(API_PARAM_DEFAULT_DATA_GET_BY_HA_NAME)
    fun findDefaultParamDataByHearingAid(@Query(API_QUERY_NAME)modelName: String) : Call<String>

    @GET(API_PARAM_DEFAULT_DATA_GET_BY_HA_NUM)
    fun findDefaultParamByHearingAid(@Query(API_QUERY_NUM)modelNumber: Int) : Call<ParamInfo>

    @GET(API_ACTIVE_PARAM_DATA_GET_BY_USER)
    fun findActiveParamDataByUser(@Query(API_QUERY_EMAIL)eMail: String, @Query(API_QUERY_PASS)password: String) : Call<String>

}