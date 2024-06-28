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
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatButton
import androidx.databinding.DataBindingUtil
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
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.viewmodel.FittingFragmentListenersViewModel
import com.ark.ParameterSpace
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
    }
    private fun initializeSelectedMemory()
    {
        mSelectedMemory = ParameterSpace.value(mBinding.selectedPosition - 1)
    }
    private fun initializeSpinnerAdapter(memoryCount: Int)
    {
        try {
            mBinding.apply {
                memoryAdapter = ArrayAdapter(requireContext(),
                    androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                    resources.getStringArray(R.array.programs).copyOfRange(0, memoryCount))

                selectedPosition = if (fittingConfig.isLeftButtonSelected)
                    fittingConfig.deviceInfoExtMap[EarSide.Left]?.product?.currentMemory?.value()!! else
                    fittingConfig.deviceInfoExtMap[EarSide.Right]?.product?.currentMemory?.value()!!
            }
        } catch (ex: Throwable) {
            Log.e("initializeSpinnerAdapter", ex.message!!)
        }
    }
    private fun initializeSpinner()
    {
        val memoryCount = with(configuration) {
            if (isDoubleDeviceConnection)
                with(fittingConfig) {
                    if (isLeftButtonSelected)
                        doubleDeviceMap[EarSide.Left]?.numberOfMemories ?: 8
                    else
                        doubleDeviceMap[EarSide.Right]?.numberOfMemories ?: 8
                }
            else
                hearingAid.numberOfMemories
        }

        initializeSpinnerAdapter(memoryCount)
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun burnParametersToSelectedMemory()
    {
        GlobalScope.launch {
            try {
                with(fittingConfig) {
                    if (selectedEarSide == EarSide.Left)
                        deviceInfoExtMap[EarSide.Left]?.product?.burnChangesToMemory(mSelectedMemory)
                    else
                        deviceInfoExtMap[EarSide.Right]?.product?.burnChangesToMemory(
                            mSelectedMemory
                        )
                }
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
                with(fittingConfig) {
                    if (selectedEarSide == EarSide.Left)
                        deviceInfoExtMap[EarSide.Left]?.product?.burnChangesToAllMemories(
                            if (configuration.isDoubleDeviceConnection)
                                configuration.doubleDeviceMap[EarSide.Left]!!
                            else
                                configuration.hearingAid
                        )
                    else
                        deviceInfoExtMap[EarSide.Right]?.product?.burnChangesToAllMemories(
                            if (configuration.isDoubleDeviceConnection)
                                configuration.doubleDeviceMap[EarSide.Right]!!
                            else
                                configuration.hearingAid
                        )
                }
            } catch (ex: Throwable) {
                Log.e("burnParametersToAllMemories", ex.message!!)
            }
        }
    }
    private class FittingHandler(fragment: FittingFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {
            when (msg.what) {

            }
        }
    }

    fun equalizerBassSliderValueChanged(value: Float)
    {

    }
    fun equalizerMidSliderValueChanged(value: Float)
    {

    }
    fun equalizerTrebleSliderValueChanged(value: Float)
    {

    }
    fun wideBandSliderValueChanged(value: Float)
    {

    }
    fun equalizerCheckedChanged()
    {
        threadPool.execute {
            try {
                mBinding.checkBoxEqualizer.apply {
                    fittingConfig.apply {
                        if (isChecked)
                            deviceInfoExtMap[selectedEarSide]?.let {
                                it.product?.disableEqualizer(mSelectedMemory)
                            }
                        else
                            deviceInfoExtMap[selectedEarSide]?.let {
                                it.product?.enableEqualizer(mSelectedMemory)
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

        threadPool.execute { mBinding.invalidateAll() }

    }
    fun spinnerItemSelected(pos: Int)
    {
        mSelectedMemory = ParameterSpace.value(pos)
        mBinding.selectedPosition = pos
        threadPool.execute { mBinding.invalidateAll() }

    }

}