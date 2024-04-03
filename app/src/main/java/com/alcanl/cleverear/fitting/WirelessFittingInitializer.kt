package com.alcanl.cleverear.fitting

import android.content.Context
import android.util.Log
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
    private lateinit var mLibrary: Library
    fun startInitializer() : Future<*>
    {
        return threadPool.submit {

            try {
                initializeLibrary()
                createProducts()
                initializeDevice()
            } catch (ex: IOException) {
                Log.e("Initializer", ex.stackTraceToString())
            } catch (ex: ArkException) {
                Log.e("Initializer", ex.stackTraceToString())
            }
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
    fun initControls() {
        threadPool.execute {
            try {

                val product = mLibrary.products.getItem(1).createProduct()
                val device = configuration.hearingAid.communicationAdaptor?.detectDevice()
                product.initializeDevice(configuration.hearingAid.communicationAdaptor!!)

                Log.e("test", product.deviceMACAddress)
                Log.e("test", product.memories.getItem(configuration.hearingAid.currentMemory?.value()!!)
                    .parameters.getItem(0).value.toString())
                Log.e("test", product.systemMemory.parameters.getById("X_Volume").id)
                Log.e("test", product.systemMemory.parameters.getById("X_Volume").value.toString())



            } catch (ex: IOException) {
                Log.e("IO", ex.stackTraceToString())
            } catch (ex: ArkException) {
                Log.e("ARK", ex.stackTraceToString())
            }
        }
    }
}