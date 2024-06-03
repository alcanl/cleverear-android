package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.api.dto.HearingAidDTO
import com.alcanl.cleverear.databinding.FragmentHomeBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.WirelessFittingInitializer
import com.alcanl.cleverear.fitting.exception.NetworkException
import com.alcanl.cleverear.fitting.readAllParameters
import com.alcanl.cleverear.fitting.readIntParameter
import com.alcanl.cleverear.fitting.setParameter
import com.alcanl.cleverear.util.WHAT_LIBRARY_NOT_FOUND
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_FAIL
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_SUCCESS
import com.alcanl.cleverear.util.WHAT_LOADING_PARAM
import com.alcanl.cleverear.util.WHAT_PARAM_LOAD_SUCCESS
import com.alcanl.cleverear.util.WHAT_PARAM_NOT_FOUND
import com.alcanl.cleverear.util.WHAT_PARAM_SEARCH_SUCCESS
import com.alcanl.cleverear.util.WHAT_PRODUCT_CREATE_FAIL
import com.alcanl.cleverear.util.WHAT_PRODUCT_CREATE_SUCCESS
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_FAIL
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_PROCESS
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_SUCCESS
import com.alcanl.cleverear.viewmodel.HomeFragmentListenersViewModel
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
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject
@AndroidEntryPoint
class HomeFragment : Fragment() {
    @Inject
    lateinit var wirelessFittingInitializer: WirelessFittingInitializer
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var threadPool: ExecutorService
    private var mDeviceInfo: DeviceInfo? = null
    private var mHearingAidDTO: HearingAidDTO? = null
    private lateinit var mHandler: RemoteHandler
    private lateinit var mLibrary: Library
    private lateinit var mBinding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        mBinding.emptyString = ""
        initialize()
        return mBinding.root
    }
    override fun onResume()
    {
        super.onResume()
        if (mDeviceInfo == null)
            initializeProduct()
        else {
            with(mBinding) {
                progressBar.visibility = View.INVISIBLE
                buttonRetry.visibility = View.INVISIBLE
            }
        }

    }
    private fun initialize()
    {
        mBinding.viewModel = HomeFragmentListenersViewModel(this)
        mHandler = RemoteHandler(this)
    }
    private suspend fun initializeProductCallback()
    {
        try {
            mDeviceInfo = configuration.hearingAid.communicationAdaptor?.detectDevice()
            Log.d("LIBRARY_ID", mDeviceInfo?.libraryId.toString())
            val libFile =
                wirelessFittingInitializer.findLibraryByLibraryId(mDeviceInfo?.libraryId?.toString()!!)
            mHearingAidDTO = wirelessFittingInitializer.findHearingAidDTO(mDeviceInfo!!)

            if (libFile != null && mHearingAidDTO?.modelName?.isNotEmpty()!!) {
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
                    mHandler.sendEmptyMessage(WHAT_READ_PARAMETERS_PROCESS)

                    if (product.readAllParameters(configuration.hearingAid))
                        mHandler.sendEmptyMessage(WHAT_READ_PARAMETERS_SUCCESS)
                    else
                        mHandler.sendEmptyMessage(WHAT_READ_PARAMETERS_FAIL)

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
    private suspend fun loadDefaultParamCallBack()
    {
        try {
            val paramFile = wirelessFittingInitializer.findDefaultParamByHearingAid(mDeviceInfo?.serialId!!)

            if (paramFile != null) {
                mHandler.sendEmptyMessage(WHAT_PARAM_SEARCH_SUCCESS)
                runBlocking {
                    delay(2000)
                }
                mHandler.sendEmptyMessage(WHAT_LOADING_PARAM)
                with(configuration) {
                    val path = File(context?.filesDir, "default_${mDeviceInfo?.serialId}.param").absolutePath
                    product.loadParamFile(path, true, false, false)
                }
                runBlocking {
                    delay(1000)
                }
                mHandler.sendEmptyMessage(WHAT_PARAM_LOAD_SUCCESS)
            }
            else
                mHandler.sendEmptyMessage(WHAT_PARAM_NOT_FOUND)

        } catch (ex: NetworkException) {
            mHandler.sendEmptyMessage(WHAT_LIBRARY_SEARCH_FAIL)
            Log.e("NETWORK_ERR", ex.message!!)
        } catch (ex: ArkException) {
            Log.e("ARK_ERR", ex.message!!)
            mHandler.sendEmptyMessage(WHAT_PRODUCT_CREATE_FAIL)
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeProduct()
    {
        GlobalScope.launch { initializeProductCallback() }
    }
    fun testParameterButtonClicked()
    {
        threadPool.execute {
            try {

                Log.e("TEST_PARAMETER", configuration.product.readIntParameter("X_VolumeRetention_Enable").toString())
                configuration.product.setParameter("X_VolumeRetention_Enable", 0)

            } catch (ex: ArkException) { Log.e("TEST_PARAMETER", ex.message!!)}
        }
    }
    private class RemoteHandler(fragment: HomeFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)
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

                    textViewInitialize.text = mWeakReference.get()?.getText(R.string.textview_initialize_success_text)
                    it.mBinding.invalidateAll()
                    frameLayoutDeviceMac.visibility = View.VISIBLE
                    frameLayoutDeviceMac.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.translate_ear_button_up_to_down))
                    frameLayoutDeviceModel.visibility = View.VISIBLE
                    frameLayoutDeviceModel.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.translate_ear_button_up_to_down))
                    frameLayoutLibraryId.visibility = View.VISIBLE
                    frameLayoutLibraryId.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.translate_ear_button_up_to_down))
                    frameLayoutFirmwareId.visibility = View.VISIBLE
                    frameLayoutFirmwareId.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.translate_ear_button_up_to_down))
                    frameLayoutFirmwareVersion.visibility = View.VISIBLE
                    frameLayoutFirmwareVersion.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.translate_ear_button_up_to_down))
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
            mWeakReference.get()?.let {
                it.mBinding.progressBar.visibility = View.INVISIBLE
                it.mBinding.textViewInitialize.text = "The Parameters Has Successfully Read From The Device"
            }
        }
        private fun readParameterFail()
        {
            mWeakReference.get()?.let {
                it.mBinding.textViewInitialize.text = "Unknown Problem Occurred While Reading Parameters, Please Contact With Your Delegate"
            }
        }
        private fun paramSearchSuccess()
        {

        }
        private fun paramLoading()
        {

        }
        private fun paramNotFound()
        {

        }
        private fun paramLoadSuccess()
        {

        }
        private fun readingParameters()
        {
            mWeakReference.get()?.let {
                it.mBinding.textViewInitialize.text = "Please Wait While Reading Inscribed Parameters From The Device"
            }
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
                17 -> paramSearchSuccess()
                18 -> paramLoading()
                19 -> paramNotFound()
                20 -> paramLoadSuccess()
                21 -> readingParameters()
            }
        }
    }
    fun retryButtonClicked()
    {
        with(mBinding) {
            textViewInitialize.text = getText(R.string.textview_initializing_text)
            buttonRetry.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        }
        initializeProduct()
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun loadDefaultParamButtonClicked()
    {
        GlobalScope.launch { loadDefaultParamCallBack() }
    }
}
