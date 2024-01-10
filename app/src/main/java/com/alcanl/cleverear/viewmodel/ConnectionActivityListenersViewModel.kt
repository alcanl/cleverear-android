package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.ConnectionActivity
import java.lang.ref.WeakReference

class ConnectionActivityListenersViewModel(activity: ConnectionActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleDetectDeviceButton()
    {
        mWeakReference.get()?.buttonDetectDevice()
    }
    fun handleBackButton()
    {
        mWeakReference.get()?.buttonBack()
    }
    fun handleListViewItemSelected(pos: Int)
    {
        mWeakReference.get()?.hearingAidItemClicked(pos)
    }
    fun handleStopScanButton()
    {
        mWeakReference.get()?.stopScanButtonClicked()
    }
}