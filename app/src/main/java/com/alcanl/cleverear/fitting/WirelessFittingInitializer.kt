package com.alcanl.cleverear.fitting

import android.content.Context
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.api.IEarTechnicLibraryService
import com.alcanl.cleverear.api.aid.HearingAidDTO
import com.alcanl.cleverear.api.equalizer.EqualizerValuesDTO
import com.alcanl.cleverear.api.library.LibraryDTO
import com.alcanl.cleverear.api.param.ParamDTO
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.fitting.exception.NetworkException
import com.alcanl.cleverear.util.getEqualizerDTOPref
import com.alcanl.cleverear.util.getEqualizerValuesModelSerial
import com.alcanl.cleverear.util.getHearingAidDTOFromPrefs
import com.alcanl.cleverear.util.getHearingAidSerialNumber
import com.alcanl.cleverear.util.saveEqualizerDTOPref
import com.alcanl.cleverear.util.saveHearingAidDTOPref
import com.ark.DeviceInfo
import com.ark.Library
import com.ark.Product
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WirelessFittingInitializer @Inject constructor(@ApplicationContext context: Context) {

    private val mContext = context

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var fittingConfig: FittingConfigurationModule

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
            Log.e(WirelessFittingInitializer::class.simpleName, ex.message!!)
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
                paramFile = writeDataToFilesDir(it.convertParamData(), "default_$modelNumber.param").await()
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
                libFile = writeDataToFilesDir(it.convertLibraryData(), "$idHex.library").await()
            }
        } catch (ex: Throwable) {
            throw NetworkException(ex)
        }

        return libFile
    }

    private fun writeDataToFilesDir(byteArray: ByteArray, fileName: String) : Deferred<File?>
    {
        return CoroutineScope(Dispatchers.IO).async { writeDataToFilesDirCallback(byteArray, fileName) }
    }

    fun createProduct(productId : Int, library: Library): Product = library.products.getById(productId).createProduct()

    suspend fun findLibraryByLibraryId(libraryId: String) : File?
    {
        try {
            val idHex = Integer.toHexString(libraryId.toInt())
            val libFile = File(mContext.filesDir, "$idHex.library")

            return if (libFile.exists()) libFile else getLibraryFromService(libraryId)

        } catch (ex: NumberFormatException) {
            Log.e(WirelessFittingInitializer::class.simpleName, ex.message!!)
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

            if (getHearingAidSerialNumber(mContext, configuration, fittingConfig).isEmpty()) {
                hearingAidDTO = getHearingAidDTOFromService(deviceInfo.serialId)
                saveHearingAidDTOPref(mContext, configuration, fittingConfig, hearingAidDTO)
                return hearingAidDTO
            }

            return getHearingAidDTOFromPrefs(mContext, configuration, fittingConfig)

        } catch (ex: Exception) {
            Log.e(WirelessFittingInitializer::class.simpleName, ex.message!!)
            return null
        }
    }

    suspend fun findEqualizerValues(hearingAidDTO: HearingAidDTO?) : EqualizerValuesDTO?
    {
        try {
            val equalizerValuesDTO: EqualizerValuesDTO?

            if (getEqualizerValuesModelSerial(mContext, configuration, fittingConfig).isEmpty()) {
                equalizerValuesDTO = getEqualizerValuesDTOFromService(hearingAidDTO?.equalizerValuesId!!)
                saveEqualizerDTOPref(mContext, configuration, fittingConfig, equalizerValuesDTO)
                return equalizerValuesDTO
            }

            return getEqualizerDTOPref(mContext, configuration, fittingConfig, hearingAidDTO?.wdrcChannelCount!!)

        } catch (ex: Exception) {
            Log.e(WirelessFittingInitializer::class.simpleName, ex.message!!)
            return null
        }
    }
}