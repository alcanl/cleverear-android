package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fragment.ConnectionFragment
import java.lang.ref.WeakReference

class ConnectionFragmentListenersViewModel(fragment: ConnectionFragment) {
    private val mWeakReference = WeakReference(fragment)

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
    fun handleStopConnectionButton()
    {
        mWeakReference.get()?.stopConnectionButtonClicked()
    }
}