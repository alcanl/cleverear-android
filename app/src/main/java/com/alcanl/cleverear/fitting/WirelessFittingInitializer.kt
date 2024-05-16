package com.alcanl.cleverear.fitting

import android.content.Context
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.api.IEarTechnicLibraryService
import com.alcanl.cleverear.api.library.LibraryInfo
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.exception.NetworkException
import com.ark.Library
import com.ark.Product
import com.ark.ProductManager
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
    lateinit var productManager: ProductManager
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var earTechnicService: IEarTechnicLibraryService
    private fun writeLibraryToFilesDirCallback(byteArray: ByteArray, fileName: String) : File?

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
    private suspend fun getLibraryFromService(libId: String) : File?
    {
        val call = earTechnicService.findLibraryByLibraryId(libId)
        val libraryInfo: LibraryInfo?
        var libFile : File? = null
        call.timeout().timeout(20, TimeUnit.SECONDS)
        call.timeout().deadline(20, TimeUnit.SECONDS)
        try {
            val response = call.awaitResponse()
            libraryInfo = response.body()
            libraryInfo?.let {
                val idHex = Integer.toHexString(libId.toInt())
                libFile = writeLibraryToFilesDir(it.convertLibraryData(), "$idHex.library").get()
            }
        } catch (ex: Throwable) {
            throw NetworkException(ex)
        }

        return libFile
    }
    private fun writeLibraryToFilesDir(byteArray: ByteArray, fileName: String) : Future<File?>
    {
        return threadPool.submit(Callable { writeLibraryToFilesDirCallback(byteArray, fileName) })
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

}