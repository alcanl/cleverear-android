package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fragment.ConnectionFragment
import java.lang.ref.WeakReference

class ConnectionFragmentListenersViewModel(fragment: ConnectionFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleDetectDeviceButton()
    {
        mWeakReference.get()?.onDetectDeviceButtonClicked()
    }
    fun handleBackButton()
    {
        mWeakReference.get()?.onBackButtonClicked()
    }
    fun handleListViewItemSelected(pos: Int)
    {
        mWeakReference.get()?.onHearingAidItemClicked(pos)
    }
    fun handleStopConnectionButton()
    {
        mWeakReference.get()?.onStopConnectionButtonClicked()
    }
}