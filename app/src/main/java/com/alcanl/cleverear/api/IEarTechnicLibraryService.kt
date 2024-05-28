package com.alcanl.cleverear.api

import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.alcanl.cleverear.api.library.LibraryInfo
import com.alcanl.cleverear.api.param.ParamInfo
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
    @GET(API_HEARING_AID_DTO_GET_BY_NUM)
    fun findHearingAidPropertiesByModelNumber(@Query(API_QUERY_NUM)modelNumber: Int) : Call<HearingAidDTO>

}