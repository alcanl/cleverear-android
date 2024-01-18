package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.ConnectionActivity
import java.lang.ref.WeakReference

class ConnectionActivityListenersViewModel(activity: ConnectionActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleDetectDeviceButton()
    {
        mWeakReference.get()?.buttonDetectDeviceClicked()
    }
    fun handleBackButton()
    {
        mWeakReference.get()?.buttonBackClicked()
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