package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fragment.BeaconFragment
import java.lang.ref.WeakReference

class BeaconFragmentListenersViewModel(fragment: BeaconFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleDetectDeviceButton()
    {
        mWeakReference.get()?.enableEddystoneTracking()
    }
}