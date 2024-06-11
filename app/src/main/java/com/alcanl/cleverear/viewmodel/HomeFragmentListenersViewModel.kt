package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fitting.fragment.HomeFragment
import com.alcanl.cleverear.util.EarSide
import java.lang.ref.WeakReference

class HomeFragmentListenersViewModel(fragment: HomeFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleRetryButton()
    {
        mWeakReference.get()?.retryButtonClicked()
    }
    fun handleEarSideButtons(earSide: EarSide)
    {
        mWeakReference.get()?.commonEarButtonsClicked(earSide)
    }
}