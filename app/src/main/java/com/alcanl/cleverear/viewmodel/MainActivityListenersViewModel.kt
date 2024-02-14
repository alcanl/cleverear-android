package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.MainActivity
import java.lang.ref.WeakReference

class MainActivityListenersViewModel (activity: MainActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleSingleDeviceButton()
    {
        mWeakReference.get()?.buttonSingleDeviceClick()
    }
    fun handleDoubleDeviceButton()
    {
        mWeakReference.get()?.buttonDoubleDeviceClick()
    }
    fun handleControlButton()
    {
        mWeakReference.get()?.buttonControlClicked()
    }
}