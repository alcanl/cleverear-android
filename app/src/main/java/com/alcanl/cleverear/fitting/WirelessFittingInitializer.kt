package com.alcanl.cleverear.fitting

import android.content.Context
import android.os.Build
import android.util.Log
import com.alcanl.cleverear.api.IEarTechnicLibraryService
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.ark.ArkException
import com.ark.Library
import com.ark.ProductManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    fun writeLibraryToCacheDir(byteArray: ByteArray)
    {
        threadPool.execute { writeLibraryToCacheDirCallback(byteArray) }
    }
    private fun writeLibraryToCacheDirCallback(byteArray: ByteArray)

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
        val file = File(mContext.cacheDir, "test10.library")
        mLibrary = productManager.loadLibraryFromFile(file.absolutePath)
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
                    hearingAid.communicationAdaptor?.let {
                        val test = it.detectDevice()

                        Log.d("deviceInfo", test.libraryId.toString())
                        //productLeft.initializeDevice(it)
                    }
                else
                    hearingAid.communicationAdaptor?.let{
                        it.detectDevice()
                        productRight.initializeDevice(it)
                    }

        }
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
    fun findLibraryByModelName(modelName: String) = earTechnicService.findLibraryByHearingAidModelName(modelName)
    fun findLibraryByLibraryId(libraryId: String) = earTechnicService.findLibraryByLibraryId(libraryId)
}