package com.alcanl.cleverear

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.WirelessFittingInitializer
import com.alcanl.cleverear.fitting.exception.NetworkException
import com.alcanl.cleverear.util.WHAT_LIBRARY_NOT_FOUND
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_FAIL
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_SUCCESS
import com.alcanl.cleverear.util.WHAT_PRODUCT_CREATE_FAIL
import com.alcanl.cleverear.util.WHAT_PRODUCT_CREATE_SUCCESS
import com.alcanl.cleverear.viewmodel.RemoteFittingActivityListenersViewModel
import com.ark.ArkException
import com.ark.DeviceInfo
import com.ark.Library
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class RemoteFittingActivity : AppCompatActivity() {
    @Inject
    lateinit var wirelessFittingInitializer: WirelessFittingInitializer
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var threadPool: ExecutorService

    private var mDeviceInfo: DeviceInfo? = null
    private lateinit var mHandler: RemoteHandler
    private lateinit var mLibrary: Library
    private lateinit var mBinding: ActivityRemoteFittingBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }
    override fun onStart()
    {
        super.onStart()
        initializeProduct()
    }
    private fun initialize()
    {
        initBinding()
        mHandler = RemoteHandler(this)
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_remote_fitting)
        mBinding.viewModel = RemoteFittingActivityListenersViewModel(this)
    }
    private suspend fun initializeProductCallback()
    {
        try {
            mDeviceInfo = configuration.hearingAid.communicationAdaptor?.detectDevice()
            Log.d("LIBRARY_ID", mDeviceInfo?.libraryId.toString())
            val libFile =
                wirelessFittingInitializer.findLibraryByLibraryId(mDeviceInfo?.libraryId?.toString()!!)

            if (libFile != null) {
                mHandler.sendEmptyMessage(WHAT_LIBRARY_SEARCH_SUCCESS)
                runBlocking {
                    delay(2000)
                }
                mLibrary = productManager.loadLibraryFromFile(libFile.absolutePath)
                with(configuration) {
                    product = wirelessFittingInitializer.createProduct(mDeviceInfo!!.productId, mLibrary)
                    product.initializeDevice(hearingAid.communicationAdaptor)
                    mHandler.sendEmptyMessage(WHAT_PRODUCT_CREATE_SUCCESS)
                    runBlocking {
                        delay(2000)
                    }
                }
            }
            else
                mHandler.sendEmptyMessage(WHAT_LIBRARY_NOT_FOUND)

        } catch (ex: NetworkException) {
            Log.e("NETWORK", ex.message!!)
            mHandler.sendEmptyMessage(WHAT_LIBRARY_SEARCH_FAIL)
        } catch (ex: ArkException) {
            Log.e("ARK", ex.message!!)
            mHandler.sendEmptyMessage(WHAT_PRODUCT_CREATE_FAIL)
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeProduct()
    {
        GlobalScope.launch { initializeProductCallback() }
    }
    private class RemoteHandler(activity: RemoteFittingActivity) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(activity)
        private fun handleInitProcessViews(text : String = "")
        {
            mWeakReference.get()?.let {
                if (text.isNotEmpty())
                    it.mBinding.textViewInitialize.text = text

                it.mBinding.progressBar.visibility = View.INVISIBLE
                it.mBinding.buttonRetry.visibility = View.VISIBLE
            }
        }
        private fun productCreateSuccess()
        {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    progressBar.visibility = View.INVISIBLE
                    textViewInitialize.text = mWeakReference.get()?.getText(R.string.textview_initialize_success_text)
                    textViewDeviceMac.text = it.configuration.product.deviceMACAddress
                    textViewDeviceModel.text = it.mDeviceInfo?.serialId.toString()
                    textViewLibraryId.text = Integer.toHexString(it.mDeviceInfo?.libraryId!!.toInt())
                    textViewFirmwareId.text = it.mDeviceInfo?.firmwareId
                    textViewFirmwareVersion.text = it.mDeviceInfo?.firmwareVersion.toString()
                    linearLayoutMain.visibility = View.VISIBLE
                }
            }
        }
        private fun productCreateFail()
        {
            handleInitProcessViews(mWeakReference.get()?.getString(R.string.textview_initialize_unknown_problem_text)!!)
        }
        private fun libraryNotFound()
        {
            handleInitProcessViews(mWeakReference.get()?.getString(R.string.textview_initialize_library_not_found_text)!!)
        }
        private fun librarySearchFail()
        {
            handleInitProcessViews(mWeakReference.get()?.getString(R.string.textview_initialize_network_problem_text)!!)
        }
        private fun librarySearchSuccess()
        {
            mWeakReference.get()?.let {
                it.mBinding.textViewInitialize.text = mWeakReference.get()?.getText(R.string.textview_initialize_library_found_text)
            }
        }
        private fun readParameterSuccess()
        {

        }
        private fun readParameterFail()
        {

        }
        override fun handleMessage(msg: Message)
        {
            when (msg.what) {
                10 -> productCreateSuccess()
                11 -> productCreateFail()
                12 -> librarySearchSuccess()
                13 -> librarySearchFail()
                14 -> readParameterSuccess()
                15 -> readParameterFail()
                16 -> libraryNotFound()
            }
        }
    }
    fun retryButtonClicked()
    {
        mBinding.buttonRetry.visibility = View.INVISIBLE
        mBinding.progressBar.visibility = View.VISIBLE
        initializeProduct()
    }
}