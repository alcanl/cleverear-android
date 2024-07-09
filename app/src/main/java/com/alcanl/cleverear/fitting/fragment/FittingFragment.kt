package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentFittingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.fitting.burnChangesToAllMemories
import com.alcanl.cleverear.fitting.burnChangesToMemory
import com.alcanl.cleverear.fitting.disableEqualizer
import com.alcanl.cleverear.fitting.disableFeedBackCanceler
import com.alcanl.cleverear.fitting.disableNoiseReduction
import com.alcanl.cleverear.fitting.enableEqualizer
import com.alcanl.cleverear.fitting.enableFeedBackCanceler
import com.alcanl.cleverear.fitting.enableNoiseReduction
import com.alcanl.cleverear.fitting.setWideBandGain
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_DISABLE_EQUALIZER_SLIDERS
import com.alcanl.cleverear.util.WHAT_ENABLE_EQUALIZER_SLIDERS
import com.alcanl.cleverear.util.WHAT_HIDE_LOADING_FRAME
import com.alcanl.cleverear.util.WHAT_SHOW_LOADING_FRAME
import com.alcanl.cleverear.viewmodel.FittingFragmentListenersViewModel
import com.ark.ParameterSpace
import com.skydoves.powerspinner.IconSpinnerItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject


@AndroidEntryPoint
class FittingFragment : Fragment() {
    @Inject
    lateinit var fittingConfig: FittingConfigurationModule
    @Inject
    lateinit var threadPool : ExecutorService
    @Inject
    lateinit var configuration: ConfigurationModule

    private lateinit var mSelectedMemory : ParameterSpace
    private lateinit var mBinding: FragmentFittingBinding
    private lateinit var mHandler: FittingHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_fitting, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize()
    {
        mBinding.viewModel = FittingFragmentListenersViewModel(this)
        mHandler = FittingHandler(this)
        initializeSpinner()
        initializeSelectedMemory()
        initializeEqualizer()
    }
    private fun initializeSelectedMemory()
    {
        mSelectedMemory = ParameterSpace.value(mBinding.selectedPosition - 1)
    }
    private fun initializeEqualizer()
    {
        mBinding.isEqualizerEnabled = with(fittingConfig) {
            if (isLeftButtonSelected)
                deviceInfoExtMap[EarSide.Left]?.isEqualizerEnabled()!!
            else
                deviceInfoExtMap[EarSide.Right]?.isEqualizerEnabled()!!
        }

    }
    private fun initializeSpinnerAdapter(memoryCount: Int)
    {
        threadPool.execute {
            try {
                mBinding.apply {
                    val iconAdapterList = ArrayList<IconSpinnerItem>(memoryCount)
                    val itemArray = resources.getStringArray(R.array.programs)

                    (0..< memoryCount).forEach {
                        iconAdapterList.add(IconSpinnerItem(text = itemArray[it]))
                    }
                    spinnerMemory.setItems(iconAdapterList)

                    selectedPosition = if (fittingConfig.isLeftButtonSelected)
                        fittingConfig.deviceInfoExtMap[EarSide.Left]?.product?.currentMemory?.value()!! else
                        fittingConfig.deviceInfoExtMap[EarSide.Right]?.product?.currentMemory?.value()!!
                }
            } catch (ex: Throwable) {
                Log.e("initializeSpinnerAdapter", ex.message!!)
            }
        }
    }
    private fun initializeSpinner()
    {
        val memoryCount = with(configuration) {
            if (isDoubleDeviceConnection)
                with(fittingConfig) {
                    if (isLeftButtonSelected)
                        hearingAidMap[EarSide.Left]?.numberOfMemories ?: 8
                    else
                        hearingAidMap[EarSide.Right]?.numberOfMemories ?: 8
                }
            else
                hearingAidMap[fittingConfig.selectedEarSide]?.numberOfMemories ?: 8
        }

        initializeSpinnerAdapter(memoryCount)
    }

    @Synchronized
    private fun wideBandSliderValueChangedCallback(value: Int)
    {
        try {
            fittingConfig.apply {
                deviceInfoExtMap[selectedEarSide]?.let {
                    it.product?.setWideBandGain(value)
                }
            }
        } catch (ex: Throwable) {
            Log.e("wideBandSliderValueChanged", ex.message!!)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun burnParametersToSelectedMemory()
    {
        GlobalScope.launch {
            try {
                mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
                with(fittingConfig) {
                    if (selectedEarSide == EarSide.Left)
                        deviceInfoExtMap[EarSide.Left]?.product?.burnChangesToMemory(mSelectedMemory)
                    else
                        deviceInfoExtMap[EarSide.Right]?.product?.burnChangesToMemory(
                            mSelectedMemory
                        )
                }
                mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)
            } catch (ex: Throwable) {
                Log.e("burnParametersToSelectedMemory", ex.message!!)
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun burnParametersToAllMemories()
    {
        GlobalScope.launch {
            try {
                mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
                with(fittingConfig) {
                    if (selectedEarSide == EarSide.Left)
                        deviceInfoExtMap[EarSide.Left]?.product?.burnChangesToAllMemories(
                            if (configuration.isDoubleDeviceConnection)
                                configuration.hearingAidMap[EarSide.Left]!!
                            else
                                configuration.hearingAidMap[selectedEarSide]!!
                        )
                    else
                        deviceInfoExtMap[EarSide.Right]?.product?.burnChangesToAllMemories(
                            if (configuration.isDoubleDeviceConnection)
                                configuration.hearingAidMap[EarSide.Right]!!
                            else
                                configuration.hearingAidMap[selectedEarSide]!!
                        )
                }
                mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)
            } catch (ex: Throwable) {
                Log.e("burnParametersToAllMemories", ex.message!!)
            }
        }
    }

    private fun invalidateBindingsCallback()
    {
        mHandler.sendEmptyMessage(WHAT_SHOW_LOADING_FRAME)
        mBinding.invalidateAll()
        mHandler.sendEmptyMessage(WHAT_HIDE_LOADING_FRAME)
    }

    private class FittingHandler(fragment: FittingFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {
            when (msg.what) {
                27 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        constraintLayoutMain.isEnabled = true
                        constraintLayoutLoading.visibility = View.GONE
                    }
                }
                28 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        constraintLayoutMain.isEnabled = false
                        constraintLayoutLoading.visibility = View.VISIBLE
                    }
                }
                29 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        sliderMid.isEnabled = true
                        sliderBass.isEnabled = true
                        sliderTreble.isEnabled = true
                    }
                }
                30 -> mWeakReference.get()?.let {
                    it.mBinding.apply {
                        sliderMid.isEnabled = false
                        sliderBass.isEnabled = false
                        sliderTreble.isEnabled = false
                    }
                }
            }
        }
    }

    fun equalizerBassSliderValueChanged(value: Float)
    {
        @Synchronized
        fun handleSynchronized()
        {
            TODO("Not Implemented Yet")
        }
        handleSynchronized()
    }
    fun equalizerMidSliderValueChanged(value: Float)
    {
        @Synchronized
        fun handleSynchronized()
        {
            TODO("Not Implemented Yet")
        }
        handleSynchronized()
    }
    fun equalizerTrebleSliderValueChanged(value: Float)
    {
        @Synchronized
        fun handleSynchronized()
        {
            TODO("Not Implemented Yet")
        }
        handleSynchronized()
    }
    fun wideBandSliderValueChanged(value: Float)
    {
        threadPool.execute { wideBandSliderValueChangedCallback(value.toInt()) }
    }
    fun equalizerCheckedChanged()
    {
        threadPool.execute {
            try {
                mBinding.checkBoxEqualizer.apply {
                    fittingConfig.apply {
                        if (isChecked) {
                            mHandler.sendEmptyMessage(WHAT_DISABLE_EQUALIZER_SLIDERS)
                            deviceInfoExtMap[selectedEarSide]?.let {
                                it.product?.disableEqualizer(mSelectedMemory)
                            }
                        }
                        else {
                            mHandler.sendEmptyMessage(WHAT_ENABLE_EQUALIZER_SLIDERS)
                            deviceInfoExtMap[selectedEarSide]?.let {
                                it.product?.enableEqualizer(mSelectedMemory)
                            }
                        }
                    }
                }

            } catch (ex: Throwable) {
                Log.e("equalizerCheckedChange", ex.message!!)
            }
        }
    }
    fun noiseReductionCheckedChanged()
    {
        threadPool.execute {
            try {
                mBinding.checkBoxNoiseReduction.apply {
                    fittingConfig.apply {
                        if (isChecked)
                            deviceInfoExtMap[selectedEarSide]?.let {
                                it.product?.disableNoiseReduction(it.hearingAidDTO!!)
                            }
                        else
                            deviceInfoExtMap[selectedEarSide]?.let {
                                it.product?.enableNoiseReduction(it.hearingAidDTO!!)
                            }
                    }
                }

            } catch (ex: Throwable) {
                Log.e("noiseReductionCheckedChanged", ex.message!!)
            }
        }
    }
    fun feedbackCancelerCheckedChanged()
    {
        threadPool.execute {
            try {
                mBinding.checkBoxFeedbackCanceler.apply {
                    fittingConfig.apply {
                        if (isChecked)
                            deviceInfoExtMap[selectedEarSide]?.product?.disableFeedBackCanceler()
                        else
                            deviceInfoExtMap[selectedEarSide]?.product?.enableFeedBackCanceler()
                    }
                }

            } catch (ex: Throwable) {
                Log.e("feedbackCancelerCheckedChanged", ex.message!!)
            }
        }
    }
    fun burnButtonsClicked(button: AppCompatButton)
    {
        if (button.id == mBinding.buttonBurn.id)
            burnParametersToSelectedMemory()
        else
            burnParametersToAllMemories()
    }
    fun earSideButtonClicked(earSide: EarSide)
    {
        fittingConfig.apply {
            selectedEarSide = earSide
            isRightButtonSelected = earSide == EarSide.Right
            isLeftButtonSelected = earSide == EarSide.Left
        }

        threadPool.execute(this::invalidateBindingsCallback)

    }
    fun spinnerItemSelected(pos: Int)
    {
        mSelectedMemory = ParameterSpace.value(pos)
        mBinding.selectedPosition = pos
        threadPool.execute(this::invalidateBindingsCallback)

    }

}