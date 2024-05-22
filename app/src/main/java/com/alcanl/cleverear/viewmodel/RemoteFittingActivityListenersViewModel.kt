package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.RemoteFittingActivity
import java.lang.ref.WeakReference

class RemoteFittingActivityListenersViewModel(activity: RemoteFittingActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleRetryButton()
    {
        mWeakReference.get()?.retryButtonClicked()
    }
    fun handleTestParameterButton()
    {
        mWeakReference.get()?.testParameterButtonClicked()
    }
}