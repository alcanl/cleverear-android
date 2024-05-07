package com.alcanl.cleverear.fitting

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.alcanl.cleverear.api.IEarTechnicLibraryService
import com.alcanl.cleverear.api.library.LibraryInfo
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.ark.Library
import com.ark.Product
import com.ark.ProductManager
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    private lateinit var mLibrary: Library
    private fun writeLibraryToCacheDirCallback(byteArray: ByteArray) : File?

    {
        try {
            val file = File(mContext.cacheDir, "test10.library")
            Log.d("exist", file.exists().toString())
            if (file.createNewFile()) {
                val fos = FileOutputStream(file)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    byteArray.inputStream().transferTo(fos)
                else {
                    fos.use {
                        it.write(byteArray)
                    }
                }
            }
            return file
        } catch (ex: IOException) {
            Log.e("writeLibraryToCache", ex.message!!)
            return null
        }
    }
    private fun getLibraryFromService(libId: String) : File?
    {
        val call = earTechnicService.findLibraryByLibraryId(libId)
        var libraryInfo: LibraryInfo?
        var libFile : File? = null

        call.enqueue(object: Callback<LibraryInfo> {
            override fun onResponse(call: Call<LibraryInfo>, response: Response<LibraryInfo>)
            {
                call.timeout().timeout(30, TimeUnit.SECONDS)
                call.timeout().deadline(1, TimeUnit.MINUTES)
                libraryInfo = response.body()

                libraryInfo?.let {
                    libFile = writeLibraryToCacheDir(it.convertLibraryData()).get()
                }
            }
            override fun onFailure(call: Call<LibraryInfo>, ex: Throwable)
            {
                Log.e("FailOnTransferData", ex.stackTraceToString())
                Toast.makeText(mContext, "Exception: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        })
        return libFile
    }
    private fun writeLibraryToCacheDir(byteArray: ByteArray) : Future<File?>
    {
        return threadPool.submit(Callable { writeLibraryToCacheDirCallback(byteArray) })
    }
    fun createProduct(productId : Int): Product = mLibrary.products.getById(productId).createProduct()

    fun findLibraryByLibraryId(libraryId: String) : File?
    {
        try {
            val idHex = Integer.toHexString(libraryId.toInt())
            val libFile = File(mContext.cacheDir, "$idHex.library")

            return if (libFile.exists()) libFile else getLibraryFromService(libraryId)

        } catch (ex: NumberFormatException) {
            Log.e("NumberFormat", ex.message!!)
            return null
        }
    }

}