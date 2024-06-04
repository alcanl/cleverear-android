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
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentHomeBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.WirelessFittingInitializer
import com.alcanl.cleverear.fitting.entity.DeviceInfoExt
import com.alcanl.cleverear.fitting.exception.NetworkException
import com.alcanl.cleverear.fitting.readAllParameters
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_CHANGE_FRAME_LAYOUTS
import com.alcanl.cleverear.util.WHAT_LEFT_EAR_INITIALIZED
import com.alcanl.cleverear.util.WHAT_LIBRARY_NOT_FOUND
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_FAIL
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_SUCCESS
import com.alcanl.cleverear.util.WHAT_PRODUCT_CREATE_FAIL
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_FAIL
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_PROCESS
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_SUCCESS
import com.alcanl.cleverear.util.WHAT_RIGHT_EAR_INITIALIZED
import com.alcanl.cleverear.util.postDelayAnimation
import com.alcanl.cleverear.viewmodel.HomeFragmentListenersViewModel
import com.ark.ArkException
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
class HomeFragment : Fragment() {
    @Inject
    lateinit var wirelessFittingInitializer: WirelessFittingInitializer
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var fittingConfig : FittingConfigurationModule

    private lateinit var mHandler: RemoteHandler
    private lateinit var mBinding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        initialize()
        return mBinding.root
    }
    override fun onResume()
    {
        super.onResume()
        if (fittingConfig.deviceInfoExtMap.isEmpty())
            initializeProduct()
        else {
            with(mBinding) {
                progressBar.visibility = View.INVISIBLE
                buttonRetry.visibility = View.INVISIBLE
                frameLayoutDeviceMac.visibility = View.VISIBLE
                frameLayoutLibraryId.visibility = View.VISIBLE
                frameLayoutFirmwareId.visibility = View.VISIBLE
                frameLayoutFirmwareVersion.visibility = View.VISIBLE
                frameLayoutDeviceModel.visibility = View.VISIBLE
            }
        }
    }
    private fun initialize()
    {
        mBinding.viewModel = HomeFragmentListenersViewModel(this)
        mHandler = RemoteHandler(this)
    }
    private suspend fun initializeProductCallback(hearingAid: HearingAid)
    {
        try {
            val deviceInfo = hearingAid.communicationAdaptor?.detectDevice()
            val libFile =
                wirelessFittingInitializer.findLibraryByLibraryId(deviceInfo?.libraryId?.toString()!!)
            val hearingAidDTO = wirelessFittingInitializer.findHearingAidDTO(deviceInfo)

            runBlocking {
                delay(1000)
            }

            if (libFile != null && hearingAidDTO?.modelName?.isNotEmpty()!!) {
                mHandler.sendEmptyMessage(WHAT_LIBRARY_SEARCH_SUCCESS)
                runBlocking {
                    delay(2000)
                }
                val library = productManager.loadLibraryFromFile(libFile.absolutePath)
                val product = wirelessFittingInitializer.createProduct(deviceInfo.productId, library)

                product.initializeDevice(hearingAid.communicationAdaptor)

                with(fittingConfig) {
                    if (product.ear == 0) {
                        deviceInfoExtMap[EarSide.Left] =
                            DeviceInfoExt(product, deviceInfo, hearingAidDTO, library)
                        mHandler.sendEmptyMessage(WHAT_LEFT_EAR_INITIALIZED)
                    }
                    else {
                        deviceInfoExtMap[EarSide.Right] =
                            DeviceInfoExt(product, deviceInfo, hearingAidDTO, library)
                        mHandler.sendEmptyMessage(WHAT_RIGHT_EAR_INITIALIZED)
                    }
                }

                runBlocking {
                    delay(2000)
                }

                mHandler.sendEmptyMessage(WHAT_READ_PARAMETERS_PROCESS)

                if (product.readAllParameters(hearingAid))
                    mHandler.sendEmptyMessage(WHAT_READ_PARAMETERS_SUCCESS)
                else
                    mHandler.sendEmptyMessage(WHAT_READ_PARAMETERS_FAIL)

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
        runBlocking {
            if (configuration.isDoubleDeviceConnection) {
                fittingConfig.isBinauralConnection = true
                fittingConfig.selectedEarSide = EarSide.Left
                GlobalScope.launch { initializeProductCallback(configuration.doubleDeviceMap[EarSide.Left]!!) }.join()
                GlobalScope.launch { initializeProductCallback(configuration.doubleDeviceMap[EarSide.Right]!!) }
            }
            else {
                fittingConfig.selectedEarSide = configuration.hearingAid.side
                GlobalScope.launch { initializeProductCallback(configuration.hearingAid) }
            }
        }
    }
    private fun fillTextViews(earSide: EarSide)
    {
        fittingConfig.deviceInfoExtMap[earSide]?.let {
            with(mBinding) {
                textViewDeviceMac.text = it.getDeviceMac()
                textViewDeviceModel.text = it.getDeviceModelName()
                textViewLibraryId.text = it.getDeviceLibraryId()
                textViewFirmwareId.text = it.getDeviceFirmwareId()
                textViewFirmwareVersion.text = it.getDeviceFirmwareVersion()
            }
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
        private fun startFrameAnimations()
        {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    frameLayoutDeviceMac.postDelayAnimation(0L, it.requireContext())
                    frameLayoutDeviceModel.postDelayAnimation(700L, it.requireContext())
                    frameLayoutLibraryId.postDelayAnimation(1400L, it.requireContext())
                    frameLayoutFirmwareId.postDelayAnimation(2100L, it.requireContext())
                    frameLayoutFirmwareVersion.postDelayAnimation(2800L, it.requireContext())
                }
            }
        }
        private fun productCreateSuccess()
        {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    textViewInitialize.text = mWeakReference.get()?.getText(R.string.textview_initialize_success_text)
                    if (it.configuration.isDoubleDeviceConnection)
                        it.leftEarButtonClicked()
                    else
                        it.fillTextViews(it.fittingConfig.selectedEarSide)
                }
            }
            startFrameAnimations()
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
        private fun readParameterSuccess() {
            mWeakReference.get()?.let {
                it.mBinding.progressBar.visibility = View.INVISIBLE
                it.mBinding.textViewInitialize.text =
                    it.getText(R.string.textview_parameter_read_success_text)
            }
        }
        private fun readParameterFail()
        {
            mWeakReference.get()?.let {
                it.mBinding.textViewInitialize.text = it.getText(R.string.textview_parameter_read_fail_text)
            }
        }
        private fun readingParameters()
        {
            mWeakReference.get()?.let {
                it.mBinding.textViewInitialize.text = it.getText(R.string.textview_parameter_read_process_text)
            }
        }
        private fun leftEarInitializeFinished()
        {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    imageViewLeftEar.startAnimation(AnimationUtils.loadAnimation(it.requireContext(), R.anim.alpha_blink_anim))
                    imageViewLeftEar.setImageDrawable(AppCompatResources.getDrawable(it.requireContext(), R.drawable.leftear_blue))
                    buttonLeft.isEnabled = true
                }
                it.mHandler.sendEmptyMessage(WHAT_CHANGE_FRAME_LAYOUTS)
            }
        }
        private fun rightEarInitializeFinished()
        {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    imageViewRightEar.startAnimation(AnimationUtils.loadAnimation(it.requireContext(), R.anim.alpha_blink_anim))
                    imageViewRightEar.setImageDrawable(AppCompatResources.getDrawable(it.requireContext(), R.drawable.rightear_red))
                    buttonRight.isEnabled = true
                }
                it.mHandler.sendEmptyMessage(WHAT_CHANGE_FRAME_LAYOUTS)
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
                17 -> leftEarInitializeFinished()
                18 -> rightEarInitializeFinished()
                21 -> readingParameters()
                22 -> startFrameAnimations()
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
    fun rightEarButtonClicked()
    {
        mBinding.buttonLeft.backgroundTintList = AppCompatResources.getColorStateList(this.requireContext(), com.androidplot.R.color.ap_gray)
        mBinding.buttonRight.backgroundTintList = AppCompatResources.getColorStateList(this.requireContext(), com.google.android.material.R.color.design_default_color_secondary_variant)
        fittingConfig.selectedEarSide = EarSide.Right
        fillTextViews(EarSide.Right)
        mHandler.sendEmptyMessage(WHAT_CHANGE_FRAME_LAYOUTS)
    }
    fun leftEarButtonClicked()
    {
        mBinding.buttonRight.backgroundTintList = AppCompatResources.getColorStateList(this.requireContext(), com.androidplot.R.color.ap_gray)
        mBinding.buttonLeft.backgroundTintList = AppCompatResources.getColorStateList(this.requireContext(), com.google.android.material.R.color.design_default_color_secondary_variant)
        fittingConfig.selectedEarSide = EarSide.Left
        fillTextViews(EarSide.Left)
        mHandler.sendEmptyMessage(WHAT_CHANGE_FRAME_LAYOUTS)
    }
}
