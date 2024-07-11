package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.RemoteFittingActivity
import java.lang.ref.WeakReference

class FittingUnsavedDialogListenersViewModel(activity: RemoteFittingActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleYesButton()
    {
        mWeakReference.get()?.unsavedFittingDialogPositiveButtonClicked()
    }
    fun handleNoButton()
    {
        mWeakReference.get()?.unsavedFittingDialogNegativeButtonClicked()
    }
}