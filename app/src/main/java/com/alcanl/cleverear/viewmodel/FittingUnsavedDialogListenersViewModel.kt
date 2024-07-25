package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.SelfFittingActivity
import java.lang.ref.WeakReference

class FittingUnsavedDialogListenersViewModel(activity: SelfFittingActivity) {
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