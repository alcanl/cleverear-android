package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fragment.BeaconFragment
import com.alcanl.cleverear.util.EarSide
import java.lang.ref.WeakReference

class BeaconFragmentListenersViewModel(fragment: BeaconFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleActiveEddystoneTrackingButton(earSide: EarSide)
    {
        mWeakReference.get()?.enableEddystoneTracking(earSide)
    }
}