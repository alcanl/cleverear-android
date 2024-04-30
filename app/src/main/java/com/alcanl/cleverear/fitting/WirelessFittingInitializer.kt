package com.alcanl.cleverear.fitting

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.api.IEarTechnicLibraryService
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.ark.ArkException
import com.ark.Library
import com.ark.ProductManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
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
    fun startInitializer() : Future<*>
    {
        return threadPool.submit {

            try {
                initializeLibrary()
                createProducts()
                initializeDevice()
                initControls()
            } catch (ex: IOException) {
                Log.e("Initializer", ex.stackTraceToString())
            } catch (ex: ArkException) {
                Log.e("Initializer", ex.stackTraceToString())
            }
        }
    }
    fun writeLibraryToFilesDir(byteArray: ByteArray)
    {
        threadPool.execute { writeLibraryToFilesDirCallback(byteArray) }
    }
    private fun writeLibraryToFilesDirCallback(byteArray: ByteArray)
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
        } catch (ex: IOException) {
            Log.e("writeLibraryToCache", ex.message!!)
        }
    }
    private fun initializeLibrary()
    {
        mLibrary = productManager.loadLibraryFromFile(writeLibrary())
    }
    private fun createProducts()
    {
        with(configuration) {
            if (isDoubleDeviceConnection) {
                productLeft = mLibrary.products.getItem(1).createProduct()
                productRight = mLibrary.products.getItem(1).createProduct()
            }
            else
                if (earSide == EarSide.Left)
                    productLeft = mLibrary.products.getItem(1).createProduct()
                else
                    productRight = mLibrary.products.getItem(1).createProduct()

        }
    }
    private fun initializeDevice()
    {
        with(configuration) {
            if (isDoubleDeviceConnection) {
                doubleDeviceMap[EarSide.Left]?.communicationAdaptor?.apply {
                    detectDevice()
                    productLeft.initializeDevice(this)
                }
                doubleDeviceMap[EarSide.Right]?.communicationAdaptor?.apply {
                    detectDevice()
                    productRight.initializeDevice(this)
                }
            }
            else
                if (earSide == EarSide.Left)
                    hearingAid.communicationAdaptor?.apply {
                        detectDevice()
                        productLeft.initializeDevice(this)
                    }
                else
                    hearingAid.communicationAdaptor?.apply {
                        detectDevice()
                        productRight.initializeDevice(this)
                    }

        }
    }
    private fun writeLibrary() : String
    {
        val file = File(mContext.filesDir, "test.library")
        val inputStream = mContext.resources.assets.open("test.library")
        inputStream.use {
            val fos = FileOutputStream(file.absoluteFile)
            fos.use { fileOutputStream ->
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) {
                    fileOutputStream.write(buf, 0, len)
                }
            }
        }
        return file.absolutePath
    }
    private fun initControls() {

        try {
            Log.e("test", mLibrary.libraryId.toString())
            Log.e("test", configuration.productLeft.deviceMACAddress)
            Log.e("test", configuration.productLeft.systemMemory.parameters.getById("X_Volume").id)
            Log.e("test", configuration.productLeft.systemMemory.parameters.getById("X_Volume").value.toString())



        } catch (ex: ArkException) {
            Log.e("ARK", ex.stackTraceToString())
        }

    }
    fun findLibrary(modelName: String) = earTechnicService.findLibraryByHearingAidModelName(modelName)
}