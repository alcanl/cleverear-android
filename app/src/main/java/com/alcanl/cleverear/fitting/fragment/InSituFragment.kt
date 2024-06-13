package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentInSituBinding
import com.alcanl.cleverear.di.interceptor.DecibelArrayInterceptor
import com.alcanl.cleverear.di.interceptor.FrequencyArrayInterceptor
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.extension.startAlphaAnimation
import com.alcanl.cleverear.viewmodel.InSituFragmentListenersViewModel
import com.ark.Manufacturing
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class InSituFragment : Fragment() {
    @Inject
    lateinit var fittingConfig : FittingConfigurationModule
    @Inject
    lateinit var configuration : ConfigurationModule
    @Inject
    lateinit var manufacturing: Manufacturing
    @Inject
    lateinit var threadPool : ExecutorService
    @Inject
    @FrequencyArrayInterceptor
    lateinit var frequencyArray: Array<Int>
    @Inject
    @DecibelArrayInterceptor
    lateinit var decibelArray: Array<Int>

    private var mFrequencyIndex = 0
    private var mDecibelIndex = 0
    private lateinit var mBinding : FragmentInSituBinding
    private lateinit var mHandler : InSituHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_situ, container, false)
        initialize()
        return mBinding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            textViewFrequency.text = String.format(
                getString(R.string.textview_frequency_hertz_text), frequencyArray[mFrequencyIndex])
            textViewDecibel.text = String.format(
                getString(R.string.textview_decibel_text), decibelArray[mDecibelIndex])
        }
    }
    private fun initialize()
    {
        mBinding.viewModel = InSituFragmentListenersViewModel(this)
        mBinding.config = fittingConfig
        mHandler = InSituHandler(this)
        threadPool.execute { initializeDeviceForInSituTest() }
    }
    private fun initializeDeviceForInSituTest()
    {
        /* with(fittingConfig) {
             if (configuration.isDoubleDeviceConnection) {
                 deviceInfoExtMap.forEach {
                     productManager.manufacturing.setProduct(it.value.product!!)
                 }
                 configuration.doubleDeviceMap.forEach {
                     productManager.manufacturing.initializeDevice(it.value.communicationAdaptor!!)
                 }
             }
             else {
                 if (selectedEarSide == EarSide.Left)
                     productManager.manufacturing.setProduct(deviceInfoExtMap[EarSide.Left]?.product!!)
                 else
                     productManager.manufacturing.setProduct(deviceInfoExtMap[EarSide.Right]?.product!!)

                 }*/

        manufacturing.initializeDevice(configuration.hearingAid.communicationAdaptor!!)
        manufacturing.setProduct(fittingConfig.deviceInfoExtMap[EarSide.Left]?.product!!)
        manufacturing.prepareDeviceForModeling(true)


    }
    private class InSituHandler(fragment: InSituFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {

        }

    }
    private fun changeText(textView: TextView, value: Int)
    {
        textView.startAlphaAnimation(requireContext(),
            String.format(getString(
                if (textView == mBinding.textViewFrequency) R.string.textview_frequency_hertz_text
            else R.string.textview_decibel_text), value))
    }
    fun earSideButtonClicked(earSide: EarSide)
    {
        fittingConfig.apply {
            selectedEarSide = earSide
            isRightButtonSelected = earSide == EarSide.Right
            isLeftButtonSelected = earSide == EarSide.Left
        }
        mBinding.invalidateAll()
    }
    fun playSoundButtonClicked()
    {
        manufacturing.generateTone(frequencyArray[mFrequencyIndex], decibelArray[mDecibelIndex])
    }
    fun decibelButtonsClicked(value: Int)
    {
        if (mDecibelIndex <= 0 && value < 0)
            return

        if (mDecibelIndex >= 18 && value > 0)
            return

        mBinding.apply {
            if (value > 0)
                changeText(textViewDecibel, decibelArray[++mDecibelIndex])
            else
                changeText(textViewDecibel, decibelArray[--mDecibelIndex])
        }
    }
    fun frequencyButtonsClicked(imageButton: ImageButton)
    {
        if (imageButton == mBinding.imageButtonIncreaseFrequency) {
            if (mFrequencyIndex >= frequencyArray.size - 1)
                return

            changeText(mBinding.textViewFrequency ,frequencyArray[++mFrequencyIndex])
        }
        else {
            if (mFrequencyIndex <= 0)
                return

            changeText(mBinding.textViewFrequency ,frequencyArray[--mFrequencyIndex])
        }
    }
    fun saveButtonClicked()
    {

    }
    fun autoFitButtonClicked()
    {

    }
}