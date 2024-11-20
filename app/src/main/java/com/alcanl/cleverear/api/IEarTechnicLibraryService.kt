package com.alcanl.cleverear.api

import com.alcanl.cleverear.api.aid.HearingAidDTO
import com.alcanl.cleverear.api.equalizer.EqualizerValuesDTO
import com.alcanl.cleverear.api.library.LibraryDTO
import com.alcanl.cleverear.api.param.ParamDTO
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface IEarTechnicLibraryService {

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_LIB_INFO_GET_BY_HA_URL)
    fun findLibraryByHearingAidModelName(@Query(API_QUERY_NAME)modelName: String) : Call<LibraryDTO>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_LIB_INFO_GET_BY_LIB_ID_URL)
    fun findLibraryByLibraryId(@Query(API_QUERY_ID)id: String) : Call<LibraryDTO>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_PARAM_DEFAULT_DATA_GET_BY_HA_NAME)
    fun findDefaultParamDataByHearingAid(@Query(API_QUERY_NAME)modelName: String) : Call<String>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_PARAM_DEFAULT_DATA_GET_BY_HA_NUM)
    fun findDefaultParamByHearingAid(@Query(API_QUERY_NUM)modelNumber: Int) : Call<ParamDTO>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_ACTIVE_PARAM_DATA_GET_BY_USER)
    fun findActiveParamDataByUser(@Query(API_QUERY_EMAIL)eMail: String, @Query(API_QUERY_PASS)password: String) : Call<String>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_HEARING_AID_DTO_GET_BY_NUM)
    fun findHearingAidEqualizerValuesById(@Query(API_QUERY_NUM)modelNumber: Int) : Call<HearingAidDTO>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_HEARING_AID_EQUALIZER_VALUES)
    fun findHearingAidEqualizerValuesByHearingAidModelNumber(@Query(API_QUERY_NUMBER)modelNumber: Int) : Call<EqualizerValuesDTO>

    @Headers("$API_KEY_HEADER: $API_KEY")
    @GET(API_EQUALIZER_VALUES)
    fun findHearingAidEqualizerValuesById(@Query(API_QUERY_ID)id: Long) : Call<EqualizerValuesDTO>

}