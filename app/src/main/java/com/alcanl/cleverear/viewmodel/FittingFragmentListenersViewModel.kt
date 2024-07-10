package com.alcanl.cleverear.viewmodel

import android.view.View
import androidx.appcompat.widget.AppCompatButton
import com.alcanl.cleverear.fitting.fragment.FittingFragment
import com.alcanl.cleverear.util.EarSide
import java.lang.ref.WeakReference

class FittingFragmentListenersViewModel(fragment: FittingFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleCheckBoxEqualizerCheckedChanged()
    {
        mWeakReference.get()?.equalizerCheckedChanged()
    }
    fun handleCheckBoxFeedbackCancelerCheckedChanged()
    {
        mWeakReference.get()?.feedbackCancelerCheckedChanged()
    }
    fun handleCheckBoxNoiseReductionCheckedChanged()
    {
        mWeakReference.get()?.noiseReductionCheckedChanged()
    }
    fun handleEarSideSelectButton(earSide: EarSide)
    {
        mWeakReference.get()?.earSideButtonClicked(earSide)
    }
    fun handleWideBandSliderOnValueChanged(value: Float)
    {
        mWeakReference.get()?.wideBandSliderValueChanged(value)
    }
    fun handleTrebleSliderOnValueChanged(value: Float)
    {
        mWeakReference.get()?.equalizerTrebleSliderValueChanged(value)
    }
    fun handleBassSliderOnValueChanged(value: Float)
    {
        mWeakReference.get()?.equalizerBassSliderValueChanged(value)
    }
    fun handleMidSliderOnValueChanged(value: Float)
    {
        mWeakReference.get()?.equalizerMidSliderValueChanged(value)
    }
    fun handleSpinnerItemSelect(pos: Int)
    {
        mWeakReference.get()?.spinnerItemSelected(pos)
    }
    fun handleBurnButtonsClicked(view: View)
    {
        mWeakReference.get()?.burnButtonsClicked(view as AppCompatButton)
    }
    fun handleMemoryRetentionCheckedChanged()
    {
        mWeakReference.get()?.memoryRetentionCheckedChanged()
    }
    fun handleVolumeRetentionCheckedChanged()
    {
        mWeakReference.get()?.volumeRetentionCheckedChanged()
    }
}