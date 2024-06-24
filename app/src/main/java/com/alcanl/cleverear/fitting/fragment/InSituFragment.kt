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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private lateinit var lineDataSet: LineDataSet
    private lateinit var lineData: LineData
    @Inject
    private lateinit var dataList: ArrayList<Entry>
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
    override fun onDestroy()
    {
        super.onDestroy()
        manufacturing.prepareDeviceForModeling(false)
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
    private fun initializeDeviceForInSituTest() {
        with(fittingConfig) {
            if (configuration.isDoubleDeviceConnection)
                manufacturing.initializeDevice(configuration.doubleDeviceMap[selectedEarSide]?.communicationAdaptor!!)
            else
                manufacturing.initializeDevice(configuration.hearingAid.communicationAdaptor!!)

            manufacturing.prepareDeviceForModeling(true)
            manufacturing.setProduct(deviceInfoExtMap[selectedEarSide]?.product!!)
        }
    }
    private fun test()
    {

    }

    private class InSituHandler(fragment: InSituFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {
            when(msg.what) {
                19 -> showDialog()
            }
        }
        private fun showDialog()
        {
            TODO("Not implemented yet")
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
        manufacturing.prepareDeviceForModeling(false)

        fittingConfig.apply {
            selectedEarSide = earSide
            isRightButtonSelected = earSide == EarSide.Right
            isLeftButtonSelected = earSide == EarSide.Left
        }

        threadPool.execute { initializeDeviceForInSituTest() }
        mBinding.invalidateAll()
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun playSoundButtonClicked()
    {
        GlobalScope.launch {
            async {
                manufacturing.generateTone(frequencyArray[mFrequencyIndex], decibelArray[mDecibelIndex] - 120)
                delay(2000)
            }.await()
            manufacturing.stopTone()
            //mHandler.sendEmptyMessage(WHAT_SHOW_IN_SITU_DIALOG)
        }
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