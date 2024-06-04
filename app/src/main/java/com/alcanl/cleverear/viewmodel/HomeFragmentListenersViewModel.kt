package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fitting.fragment.HomeFragment
import java.lang.ref.WeakReference

class HomeFragmentListenersViewModel(fragment: HomeFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleRetryButton()
    {
        mWeakReference.get()?.retryButtonClicked()
    }
    fun handleLeftEarButton()
    {
        mWeakReference.get()?.leftEarButtonClicked()
    }
    fun handleRightEarButton()
    {
        mWeakReference.get()?.rightEarButtonClicked()
    }
}