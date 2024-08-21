package com.alcanl.cleverear.viewmodel

import android.view.View
import com.alcanl.cleverear.fragment.AdvancedFragment
import java.lang.ref.WeakReference

class AdvancedFragmentListenersViewModel(fragment: AdvancedFragment) {
    private val mWeakReference = WeakReference(fragment)

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
    fun handleSetChannelButtonClick(view: View)
    {
        mWeakReference.get()?.setStreamAddressButtonClicked(view)
    }
    fun handleSwitchCheckedChange()
    {
        mWeakReference.get()?.switchOnCheckedChanged()
    }
}