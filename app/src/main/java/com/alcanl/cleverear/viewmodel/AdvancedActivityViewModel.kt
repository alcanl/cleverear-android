package com.alcanl.cleverear.viewmodel

import android.view.View
import com.alcanl.cleverear.AdvancedActivity
import java.lang.ref.WeakReference

class AdvancedActivityViewModel(activity: AdvancedActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleMicDualSliderValueChange(value: Float)
    {
        mWeakReference.get()?.micDualSliderValueChanged(value)
    }
    fun handleMicLeftSliderValueChange(value: Float)
    {
        mWeakReference.get()?.micLeftSliderValueChanged(value)
    }
    fun handleMicRightSliderValueChange(value: Float)
    {
        mWeakReference.get()?.micRightSliderValueChanged(value)
    }
    fun handleAuxDualSliderValueChange(value: Float)
    {
        mWeakReference.get()?.auxDualSliderValueChanged(value)
    }
    fun handleAuxLeftSliderValueChange(value: Float)
    {
        mWeakReference.get()?.auxLeftSliderValueChanged(value)
    }
    fun handleAuxRightSliderValueChange(value: Float)
    {
        mWeakReference.get()?.auxRightSliderValueChanged(value)
    }
    fun handleModeButtons(view: View)
    {
        mWeakReference.get()?.modeButtonClicked(view)
    }
    fun handleSetChannelButtonClick()
    {
        mWeakReference.get()?.setStreamAddressButtonClicked()
    }
}