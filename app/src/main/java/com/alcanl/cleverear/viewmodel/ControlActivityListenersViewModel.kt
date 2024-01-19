package com.alcanl.cleverear.viewmodel

import android.view.View
import android.widget.Button
import com.alcanl.cleverear.ControlActivity
import java.lang.ref.WeakReference

class ControlActivityListenersViewModel(activity: ControlActivity) {
    private var mWeakReference = WeakReference(activity)
    fun handleMemoryButtonClick(buttonNumber: Int, view: View)
    {
        mWeakReference.get()?.memoryButtonClicked(buttonNumber, view as Button)
    }
    fun handleLeftEarSliderValueChange(value: Float)
    {
        mWeakReference.get()?.sliderLeftEarOnValueChanged(value)
    }
    fun handleRightEarSliderValueChange(value: Float)
    {
        mWeakReference.get()?.sliderRightEarOnValueChanged(value)
    }
    fun handleSwitchCheckedChange()
    {
        mWeakReference.get()?.switchOnCheckedChanged()
    }
    fun handleDualSliderValueChange(value: Float)
    {
        mWeakReference.get()?.sliderDualOnValueChanged(value)
    }
}