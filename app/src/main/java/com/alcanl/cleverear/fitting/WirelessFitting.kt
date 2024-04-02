package com.alcanl.cleverear.fitting

import android.content.Context
import android.util.Log
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.ark.ArkException
import com.ark.ProductManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class WirelessFitting @Inject constructor(@ApplicationContext context: Context) {
    private val mContext = context
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    private fun writeLibrary() : String
    {

        val file = File(mContext.filesDir,"test.library")
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
                val library = productManager.loadLibraryFromFile(writeLibrary())
                Log.e("Library", library.description)
                for (i in 0..< library.products.count)
                    Log.e("products", library.products.getItem(i).description)

                val product = library.products.getItem(1).createProduct()
                Log.e("test", product.memories.getItem(0).parameters.getItem(0).description)
                Log.e("test", product.memories.getItem(0).parameters.getItem(0).value.toString())



            } catch (ex: IOException) {
                Log.e("IO", ex.stackTraceToString())
            } catch (ex: ArkException) {
                Log.e("ARK", ex.stackTraceToString())
            }

        }
    }
}