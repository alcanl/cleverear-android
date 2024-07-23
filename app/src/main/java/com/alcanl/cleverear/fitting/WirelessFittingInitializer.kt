package com.alcanl.cleverear.fitting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.api.IEarTechnicLibraryService
import com.alcanl.cleverear.api.PREF_ACTIVE_PARAM_ID
import com.alcanl.cleverear.api.PREF_DEFAULT_PARAM_ID
import com.alcanl.cleverear.api.PREF_EQUALIZER_ID
import com.alcanl.cleverear.api.PREF_EQUALIZER_VALUES
import com.alcanl.cleverear.api.PREF_EQUALIZER_VALUES_ID
import com.alcanl.cleverear.api.PREF_EQUALIZER_VALUES_MODEL_NAME
import com.alcanl.cleverear.api.PREF_FREQUENCY_CHANNEL_COUNT
import com.alcanl.cleverear.api.PREF_LIBRARY_ID
import com.alcanl.cleverear.api.PREF_MODEL_NAME
import com.alcanl.cleverear.api.PREF_MODEL_NUMBER
import com.alcanl.cleverear.api.PREF_WDRC_CHANNEL_COUNT
import com.alcanl.cleverear.api.aid.HearingAidDTO
import com.alcanl.cleverear.api.equalizer.EqualizerValuesDTO
import com.alcanl.cleverear.api.library.LibraryDTO
import com.alcanl.cleverear.api.param.ParamDTO
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.exception.NetworkException
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.ark.DeviceInfo
import com.ark.Library
import com.ark.Product
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WirelessFittingInitializer @Inject constructor(@ApplicationContext context: Context) {
    private val mContext = context
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var earTechnicService: IEarTechnicLibraryService
    private fun writeDataToFilesDirCallback(byteArray: ByteArray, fileName: String) : File?

    {
        try {
            val file = File(mContext.filesDir, fileName)
            val fos = FileOutputStream(file)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                byteArray.inputStream().transferTo(fos)
            else
                fos.use { it.write(byteArray) }

            return file
        } catch (ex: IOException) {
            Log.e("writeLibraryToFilesDir", ex.message!!)
            return null
        }
    }
    private suspend fun getHearingAidDTOFromService(modelNumber: Int) : HearingAidDTO?
    {
        val call = earTechnicService.findHearingAidEqualizerValuesById(modelNumber)
        call.timeout().timeout(20, TimeUnit.SECONDS)
        call.timeout().deadline(20, TimeUnit.SECONDS)
        try {
            return call.awaitResponse().body()

        } catch (ex: Throwable) {
            throw NetworkException(ex)
        }
    }
    private suspend fun getEqualizerValuesDTOFromService(id: Long) : EqualizerValuesDTO?
    {
        val call = earTechnicService.findHearingAidEqualizerValuesById(id)
        call.timeout().timeout(20, TimeUnit.SECONDS)
        call.timeout().deadline(20, TimeUnit.SECONDS)
        try {
            return call.awaitResponse().body()

        } catch (ex: Throwable) {
            throw NetworkException(ex)
        }
    }
    private suspend fun getDefaultParamFromService(modelNumber: Int) : File?
    {
        val call = earTechnicService.findDefaultParamByHearingAid(modelNumber)
        val paramInfo: ParamDTO?
        var paramFile: File? = null
        call.timeout().timeout(20, TimeUnit.SECONDS)
        call.timeout().deadline(20, TimeUnit.SECONDS)
        try {
            val response = call.awaitResponse()
            paramInfo = response.body()
            paramInfo?.let {
                paramFile = writeDataToFilesDir(it.convertParamData(), "default_$modelNumber.param").get()
            }
        } catch (ex: Throwable) {
            throw NetworkException(ex)
        }

        return paramFile
    }
    private suspend fun getLibraryFromService(libId: String) : File?
    {
        val call = earTechnicService.findLibraryByLibraryId(libId)
        val libraryInfo: LibraryDTO?
        var libFile : File? = null
        call.timeout().timeout(20, TimeUnit.SECONDS)
        call.timeout().deadline(20, TimeUnit.SECONDS)
        try {
            val response = call.awaitResponse()
            libraryInfo = response.body()
            libraryInfo?.let {
                val idHex = Integer.toHexString(libId.toInt())
                libFile = writeDataToFilesDir(it.convertLibraryData(), "$idHex.library").get()
            }
        } catch (ex: Throwable) {
            throw NetworkException(ex)
        }

        return libFile
    }
    private fun writeDataToFilesDir(byteArray: ByteArray, fileName: String) : Future<File?>
    {
        return threadPool.submit(Callable { writeDataToFilesDirCallback(byteArray, fileName) })
    }
    fun createProduct(productId : Int, library: Library): Product = library.products.getById(productId).createProduct()

    suspend fun findLibraryByLibraryId(libraryId: String) : File?
    {
        try {
            val idHex = Integer.toHexString(libraryId.toInt())
            val libFile = File(mContext.filesDir, "$idHex.library")

            return if (libFile.exists()) libFile else getLibraryFromService(libraryId)

        } catch (ex: NumberFormatException) {
            Log.e("NumberFormat", ex.message!!)
            return null
        }
    }
    suspend fun findDefaultParamByHearingAid(modelNumber: Int) : File?
    {
        val paramFile = File(mContext.filesDir, "default_$modelNumber.param")
        return if (paramFile.exists()) paramFile else getDefaultParamFromService(modelNumber)
    }
    suspend fun findHearingAidDTO(deviceInfo: DeviceInfo) : HearingAidDTO?
    {
        try {
            val hearingAidDTO: HearingAidDTO?
            val pref = mContext.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
            if (pref.getString(PREF_MODEL_NAME, "")?.isEmpty()!!) {
                hearingAidDTO = getHearingAidDTOFromService(deviceInfo.serialId)
                saveHearingAidDTOPref(pref, hearingAidDTO)
                return hearingAidDTO
            }

            return getHearingAidDTOFromPrefs(pref)

        } catch (ex: Throwable) {
            Log.e("WirelessFittingInitializer::findHearingAidDTO", ex.message!!)
            return null
        }
    }
    suspend fun findEqualizerValues(hearingAidDTO: HearingAidDTO?) : EqualizerValuesDTO?
    {
        try {
            val equalizerValuesDTO: EqualizerValuesDTO?
            val pref = mContext.getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)

            if (pref.getString(PREF_EQUALIZER_VALUES_MODEL_NAME, "")?.isEmpty()!!) {
                equalizerValuesDTO = getEqualizerValuesDTOFromService(hearingAidDTO?.equalizerValuesId!!)
                saveEqualizerDTOPref(pref, equalizerValuesDTO)
                return equalizerValuesDTO
            }

            return getEqualizerDTOPref(pref, hearingAidDTO?.wdrcChannelCount!!)

        } catch (ex: Throwable) {
            Log.e("WirelessFittingInitializer::findHearingAidDTO", ex.message!!)
            return null
        }
    }

    private fun saveHearingAidDTOPref(pref: SharedPreferences, hearingAidDTO: HearingAidDTO?)
    {
        if (hearingAidDTO == null)
            return

        val editor = pref.edit()
        editor.putString(PREF_MODEL_NAME, hearingAidDTO.modelName)
        editor.putString(PREF_LIBRARY_ID, Integer.toHexString(hearingAidDTO.libraryId.toInt()))
        editor.putString(PREF_ACTIVE_PARAM_ID, hearingAidDTO.activeParamId)
        editor.putString(PREF_DEFAULT_PARAM_ID, hearingAidDTO.defaultParamId)
        editor.putInt(PREF_WDRC_CHANNEL_COUNT, hearingAidDTO.wdrcChannelCount)
        editor.putInt(PREF_FREQUENCY_CHANNEL_COUNT, hearingAidDTO.frequencyChannelCount)
        editor.putInt(PREF_MODEL_NUMBER, hearingAidDTO.modelNumber)
        editor.putLong(PREF_EQUALIZER_VALUES_ID, hearingAidDTO.equalizerValuesId)
        editor.apply()
    }
    private fun getHearingAidDTOFromPrefs(pref: SharedPreferences) : HearingAidDTO
    {
        return HearingAidDTO(pref.getString(PREF_MODEL_NAME, "")!!, pref.getInt(PREF_MODEL_NUMBER, 0),
            pref.getString(PREF_LIBRARY_ID, "")!!, pref.getString(PREF_DEFAULT_PARAM_ID, "")!!,
            pref.getString(PREF_ACTIVE_PARAM_ID, "")!!, pref.getInt(PREF_WDRC_CHANNEL_COUNT, 0),
            pref.getInt(PREF_FREQUENCY_CHANNEL_COUNT, 0), pref.getLong(PREF_EQUALIZER_VALUES_ID, 0)
        )
    }
    @SuppressLint("DefaultLocale")
    private fun saveEqualizerDTOPref(pref: SharedPreferences, equalizerValuesDTO: EqualizerValuesDTO?) {
        if (equalizerValuesDTO == null)
            return

        val editor = pref.edit()
        editor.putLong(PREF_EQUALIZER_ID, equalizerValuesDTO.id)
        editor.putString(PREF_EQUALIZER_VALUES_MODEL_NAME, equalizerValuesDTO.equalizerValuesName)
        equalizerValuesDTO.valueList.forEachIndexed { index, i ->
            editor.putInt(String.format(PREF_EQUALIZER_VALUES, index), i)
        }
        editor.apply()
    }
    @SuppressLint("DefaultLocale")
    private fun getEqualizerDTOPref(pref: SharedPreferences, count: Int) : EqualizerValuesDTO {

        val valuesList = mutableListOf<Int>()

        (0..< count).forEach {
            valuesList.add(pref.getInt(String.format(PREF_EQUALIZER_VALUES, it), 0))
        }

        return EqualizerValuesDTO(
            pref.getLong(PREF_EQUALIZER_ID, 0),
            valuesList,
            pref.getString(PREF_EQUALIZER_VALUES_MODEL_NAME, "")!!
        )
    }
}