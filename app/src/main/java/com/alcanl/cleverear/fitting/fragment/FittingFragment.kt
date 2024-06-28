package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatButton
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentFittingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.viewmodel.FittingFragmentListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
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
        mBinding.memoryAdapter = ArrayAdapter(requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, resources.getStringArray(R.array.programs))
        mBinding.selectedPosition = if (fittingConfig.isLeftButtonSelected)
            fittingConfig.deviceInfoExtMap[EarSide.Left]?.product?.currentMemory?.value()?.plus(1)!! else
            fittingConfig.deviceInfoExtMap[EarSide.Right]?.product?.currentMemory?.value()?.plus(1)!!
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

    }
    fun noiseReductionCheckedChanged()
    {

    }
    fun feedbackCancelerCheckedChanged()
    {

    }
    fun burnButtonsClicked(button: AppCompatButton)
    {

    }
    fun earSideButtonClicked(earSide: EarSide)
    {

    }

}