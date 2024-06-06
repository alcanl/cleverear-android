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
import androidx.core.view.forEach
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.RemoteFittingActivity
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
import com.alcanl.cleverear.util.WHAT_LIBRARY_NOT_FOUND
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_FAIL
import com.alcanl.cleverear.util.WHAT_LIBRARY_SEARCH_SUCCESS
import com.alcanl.cleverear.util.WHAT_PRODUCT_CREATE_FAIL
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_FAIL
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_PROCESS
import com.alcanl.cleverear.util.WHAT_READ_PARAMETERS_SUCCESS
import com.alcanl.cleverear.util.extension.getFrameLayoutAnimationListener
import com.alcanl.cleverear.util.extension.hideFrames
import com.alcanl.cleverear.util.extension.postDelayAnimation
import com.alcanl.cleverear.util.extension.showFrames
import com.alcanl.cleverear.util.extension.startAlphaAnimation
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
        if (fittingConfig.isLeftButtonSelected || fittingConfig.isRightButtonSelected) {
            with(mBinding) {
                progressBar.visibility = View.INVISIBLE
                buttonRetry.visibility = View.INVISIBLE
                imageViewHearingAid.visibility = View.VISIBLE
                showFrames()
                textViewInitialize.text = getText(R.string.textview_initialize_success_text)
            }
        }
        else
            initializeProduct()

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
                        with(mBinding) {
                            frameLayoutLeftEar.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.alpha_blink_anim_fit_image)
                                .also { it.setAnimationListener(getFrameLayoutAnimationListener(EarSide.Left, this, mHandler)) })
                        }
                    }
                    else {
                        deviceInfoExtMap[EarSide.Right] =
                            DeviceInfoExt(product, deviceInfo, hearingAidDTO, library)
                        with(mBinding) {
                            frameLayoutRightEar.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.alpha_blink_anim_fit_image)
                                .also { it.setAnimationListener(getFrameLayoutAnimationListener(EarSide.Right, this, mHandler)) })

                        }
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
        activity?.runOnUiThread {
            (activity as RemoteFittingActivity).binding.navigationView.menu.forEach {
                it.isEnabled= false
            }
        }

        runBlocking {
            if (configuration.isDoubleDeviceConnection) {
                fittingConfig.isBinauralConnection = true
                GlobalScope.launch { initializeProductCallback(configuration.doubleDeviceMap[EarSide.Left]!!) }.join()
                GlobalScope.launch { initializeProductCallback(configuration.doubleDeviceMap[EarSide.Right]!!) }
            }
            else {
                fittingConfig.selectedEarSide = configuration.hearingAid.side
                GlobalScope.launch { initializeProductCallback(configuration.hearingAid) }
            }
        }
    }
    private class RemoteHandler(fragment: HomeFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)
        private fun handleInitProcessFail(text : String = "")
        {
            mWeakReference.get()?.let {
                if (text.isNotEmpty())
                    it.mBinding.textViewInitialize.startAlphaAnimation(it.requireContext(), text)

                it.mBinding.progressBar.visibility = View.INVISIBLE
                it.mBinding.buttonRetry.visibility = View.VISIBLE
            }
        }
        private fun startFrameAnimations()
        {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    invalidateAll()
                    frameLayoutDeviceMac.postDelayAnimation(0L, it.requireContext())
                    frameLayoutDeviceModel.postDelayAnimation(700L, it.requireContext())
                    frameLayoutLibraryId.postDelayAnimation(1400L, it.requireContext())
                    frameLayoutFirmwareId.postDelayAnimation(2100L, it.requireContext())
                    frameLayoutFirmwareVersion.postDelayAnimation(2800L, it.requireContext())
                }
            }
        }
        private fun readParameterSuccess() {
            mWeakReference.get()?.let {
                with(it.mBinding) {
                    progressBar.visibility = View.INVISIBLE
                    imageViewHearingAid.startAlphaAnimation(it.requireContext())
                    textViewInitialize.startAlphaAnimation(it.requireContext(),it.getString(R.string.textview_parameter_read_success_text))
                    postDelayed({
                        (it.activity as RemoteFittingActivity).binding.navigationView.menu.forEach {item ->
                            item.isEnabled = true
                        }
                        textViewInitialize.startAlphaAnimation(it.requireContext(), it.getString(R.string.textview_initialize_success_text))
                    }, 2000)
                }

            }
        }
        private fun leftEarInitializeFinished()
        {
            mWeakReference.get()?.let {
                with(it.fittingConfig) {
                    isLeftButtonSelected = true
                    isRightButtonSelected = false
                }
                startFrameAnimations()
            }
        }
        private fun rightEarInitializeFinished()
        {
            mWeakReference.get()?.let {
                with(it.fittingConfig) {
                    isRightButtonSelected = true
                    isLeftButtonSelected= false
                }
                startFrameAnimations()
            }
        }
        override fun handleMessage(msg: Message)
        {
            when (msg.what) {
                11 -> handleInitProcessFail(mWeakReference.get()?.getString(R.string.textview_initialize_unknown_problem_text)!!)
                12 -> { mWeakReference.get()?.let {
                        it.mBinding.textViewInitialize.startAlphaAnimation(it.requireContext(), it.getString(R.string.textview_initialize_library_found_text)) }}
                13 -> handleInitProcessFail(mWeakReference.get()?.getString(R.string.textview_initialize_network_problem_text)!!)
                14 -> readParameterSuccess()
                15 -> { mWeakReference.get()?.let {
                    it.mBinding.textViewInitialize.startAlphaAnimation(it.requireContext(), it.getString(R.string.textview_parameter_read_fail_text)) }}
                16 -> handleInitProcessFail(mWeakReference.get()?.getString(R.string.textview_initialize_library_not_found_text)!!)
                17 -> leftEarInitializeFinished()
                18 -> rightEarInitializeFinished()
                21 -> { mWeakReference.get()?.let {
                    it.mBinding.textViewInitialize.startAlphaAnimation(it.requireContext(), it.getString(R.string.textview_parameter_read_process_text)) }}
                22 -> startFrameAnimations()
            }
        }
    }
    fun retryButtonClicked()
    {
        with(mBinding) {
            textViewInitialize.startAlphaAnimation(requireContext(), getString(R.string.textview_initializing_text))
            buttonRetry.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        }
        initializeProduct()
    }
    fun commonEarButtonsClicked(earSide: EarSide)
    {
        mBinding.hideFrames()

        threadPool.execute {
            fittingConfig.apply {
                selectedEarSide = earSide
                isRightButtonSelected = earSide == EarSide.Right
                isLeftButtonSelected = earSide == EarSide.Left
            }
            activity?.runOnUiThread { mBinding.hideFrames() }
            mBinding.invalidateAll()
            mHandler.sendEmptyMessage(WHAT_CHANGE_FRAME_LAYOUTS)
        }
    }
}
