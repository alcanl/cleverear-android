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
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentInSituBinding
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.extension.startAlphaAnimation
import com.alcanl.cleverear.viewmodel.InSituFragmentListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class InSituFragment : Fragment() {
    @Inject
    lateinit var fittingConfig : FittingConfigurationModule
    @Inject
    lateinit var threadPool : ExecutorService
    @Inject
    lateinit var frequencyArray: Array<Int>

    private var mFrequencyIndex = 0
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
    private fun initialize()
    {
        mBinding.viewModel = InSituFragmentListenersViewModel(this)
        mBinding.config = fittingConfig
        mHandler = InSituHandler(this)
    }
    private class InSituHandler(fragment: InSituFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {

        }

    }
    private fun changeFrequencyText(value: Int)
    {
        mBinding.textViewFrequency.startAlphaAnimation(requireContext(),
            String.format(getString(R.string.textview_frequency_hertz_text), frequencyArray[value]))
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

    }
    fun decibelButtonsClicked(value: Int)
    {
        mBinding.apply {
            val currentVal = textViewDecibel.text.substring(0, 3).trim().toInt()
            if (currentVal <= 10 || currentVal >= 100)
                return

            textViewDecibel.startAlphaAnimation(this@InSituFragment.requireContext(),
                String.format(getString(R.string.textview_decibel_text), currentVal + value))
        }
    }
    fun frequencyButtonsClicked(imageButton: ImageButton)
    {
        if (imageButton == mBinding.imageButtonIncreaseFrequency) {
            if (mFrequencyIndex >= frequencyArray.size - 1)
                return

            changeFrequencyText(frequencyArray[mFrequencyIndex++])
        }
        else {
            if (mFrequencyIndex <= 0)
                return

            changeFrequencyText(frequencyArray[mFrequencyIndex--])
        }
    }
    fun saveButtonClicked()
    {

    }
    fun autoFitButtonClicked()
    {

    }
}