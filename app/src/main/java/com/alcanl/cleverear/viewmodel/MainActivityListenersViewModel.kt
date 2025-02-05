package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.MainActivity
import java.lang.ref.WeakReference

class MainActivityListenersViewModel (activity: MainActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleSingleDeviceButton()
    {
        mWeakReference.get()?.onButtonSingleDeviceClick()
    }
    fun handleDoubleDeviceButton()
    {
        mWeakReference.get()?.onButtonDoubleDeviceClick()
    }
    fun handleControlButton()
    {
        mWeakReference.get()?.onButtonControlClicked()
    }
    fun handleInfoButton()
    {
        mWeakReference.get()?.onButtonInfoClicked()
    }
}